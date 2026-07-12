package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;
import com.github.silent.samurai.speedy.policy.condition.ConditionContext;
import com.github.silent.samurai.speedy.policy.condition.PolicyCondition;
import com.github.silent.samurai.speedy.policy.condition.PrincipalVariableResolver;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.ResourceSelector;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// The per-request decision core, built once from the {@link SpeedyAuthContext} a request resolved
/// (see {@code ISpeedyConfiguration.authContextPerReq()}). Every policy-aware handler starts with
/// {@code ctx.find(PolicyEngine.class)}; its absence from the context means no policy was
/// configured for this request, and every caller must treat that as "no restriction" (requirement:
/// behave exactly as today when unconfigured).
public class PolicyEngine {

    private final PolicyDocument document;
    @Getter
    private final Map<String, SpeedyValue> variables;

    public PolicyEngine(SpeedyAuthContext policy) {
        this.document = policy.document();
        this.variables = PrincipalVariableResolver.resolve(policy.variables());
    }

    /// Convenience for callers that need the principal id.
    public String principalId() {
        SpeedyValue v = variables.get("principal.id");
        return v != null && !v.isNull() ? v.asText() : null;
    }

    /// Entity-level decision for CREATE/DELETE (row may be non-null so create-time conditions
    /// can inspect the incoming payload) and for the READ/UPDATE entity gate's explicit-deny check.
    public PolicyEffect decideEntity(PermissionType action, EntityMetadata entity, SpeedyEntity row) {
        return resolve(action, entity.getName(), null, entity, row, false);
    }

    /// Field-level decision for a specific row (READ omission, CREATE/UPDATE field rejection).
    public boolean isFieldAllowed(PermissionType action, EntityMetadata entity, FieldMetadata field, SpeedyEntity row) {
        return resolve(action, entity.getName(), field.getOutputPropertyName(), entity, row, false) == PolicyEffect.ALLOW;
    }

    /// Query-time (no row available) READ visibility: a conditional Allow rule does not grant,
    /// since there is no row to check its condition against — filtering/sorting on such a field
    /// is rejected rather than risk leaking information about rows the caller can't see.
    public boolean isFieldReadableUnconditional(EntityMetadata entity, FieldMetadata field) {
        return resolve(PermissionType.READ, entity.getName(), field.getOutputPropertyName(), entity, null, true)
                == PolicyEffect.ALLOW;
    }

    /// Whether the caller has any rule at all referencing this entity for this action
    /// (field-specific or whole-entity, conditional or not). Used by the entity-level gate to
    /// distinguish "no access whatsoever" from "access limited to some
    /// fields/rows" (let field/row-level checks — which can actually see the row — decide).
    public boolean entityHasAnyRule(PermissionType action, EntityMetadata entity) {
        String entityName = entity.getName();
        for (SpeedyPolicy rule : document.speedyPolicies()) {
            if (!rule.getActions().contains(action)) {
                continue;
            }
            for (ResourceSelector selector : rule.getResources()) {
                if (selector.matchesEntity(entityName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean entityHasAnyReadRule(EntityMetadata entity) {
        return entityHasAnyRule(PermissionType.READ, entity);
    }

    /// The entity-level gate's "zero access at all" check. An unconditional {@code Entity.*}
    /// DENY blocks the operation immediately because it overrides every field grant. Otherwise,
    /// a deny-by-default policy admits the request only when it has at least one Allow rule for
    /// the entity; conditional grants are evaluated later when a row is available.
    public boolean isEntirelyDenied(PermissionType action, EntityMetadata entity) {
        return hasUnconditionalWholeEntityDeny(action, entity)
                || (document.defaultEffect() == PolicyEffect.DENY && !entityHasAnyAllowRule(action, entity));
    }

    private boolean entityHasAnyAllowRule(PermissionType action, EntityMetadata entity) {
        String entityName = entity.getName();
        return document.speedyPolicies().stream().anyMatch(rule ->
                rule.effect() == PolicyEffect.ALLOW
                        && rule.getActions().contains(action)
                        && rule.getResources().stream().anyMatch(selector -> selector.matchesEntity(entityName)));
    }

    private boolean hasUnconditionalWholeEntityDeny(PermissionType action, EntityMetadata entity) {
        String entityName = entity.getName();
        return document.speedyPolicies().stream().anyMatch(rule ->
                rule.effect() == PolicyEffect.DENY
                        && rule.getActions().contains(action)
                        && rule.conditions().isEmpty()
                        && rule.getResources().stream().anyMatch(selector ->
                                selector.matchesEntity(entityName) && selector.isWholeEntity()));
    }

    /// Row-scoped READ Allow conditions for {@code entity}, translated into a query condition for
    /// WHERE-injection so rows the caller cannot read never come back from the database. Returns
    /// an empty list — meaning "inject nothing, don't restrict" — in two fail-open cases: an
    /// unconditional Allow rule already grants every row, or some conditional rule contains a
    /// condition that cannot be translated (never wrongly hide a row the caller may in fact read;
    /// per-row field omission remains the actual security boundary regardless).
    public List<Condition> rowVisibilityConditions(EntityMetadata entity) {
        String entityName = entity.getName();
        List<SpeedyPolicy> readAllows = new ArrayList<>();
        for (SpeedyPolicy rule : document.speedyPolicies()) {
            if (rule.effect() != PolicyEffect.ALLOW) {
                continue;
            }
            if (!rule.getActions().contains(PermissionType.READ)) {
                continue;
            }
            boolean matchesEntity = rule.getResources().stream().anyMatch(r -> r.matchesEntity(entityName));
            if (matchesEntity) {
                readAllows.add(rule);
            }
        }
        if (readAllows.isEmpty()) {
            return List.of();
        }
        boolean hasUnconditional = readAllows.stream().anyMatch(r -> r.conditions().isEmpty());
        if (hasUnconditional) {
            return List.of();
        }

        List<Condition> orBranches = new ArrayList<>();
        for (SpeedyPolicy rule : readAllows) {
            ConditionContext ctx = new ConditionContext(null, variables, entity);
            List<Condition> translated = new ArrayList<>();
            for (PolicyCondition condition : rule.conditions()) {
                Optional<Condition> queryCondition = condition.toQueryCondition(ctx);
                if (queryCondition.isEmpty()) {
                    return List.of();
                }
                translated.add(queryCondition.get());
            }
            orBranches.add(translated.size() == 1 ? translated.get(0) : and(translated));
        }
        return List.of(orBranches.size() == 1 ? orBranches.get(0) : or(orBranches));
    }

    private static BooleanCondition and(List<Condition> conditions) {
        BooleanCondition group = new BooleanConditionImpl(ConditionOperator.AND);
        conditions.forEach(group::addSubCondition);
        return group;
    }

    private static BooleanCondition or(List<Condition> conditions) {
        BooleanCondition group = new BooleanConditionImpl(ConditionOperator.OR);
        conditions.forEach(group::addSubCondition);
        return group;
    }

    /// Shared resolution algorithm: collect every rule matching {@code action} + resource
    /// (+ conditions, when applicable); an explicit DENY always beats an ALLOW; absent either,
    /// {@code defaultEffect} governs.
    private PolicyEffect resolve(PermissionType action, String entityName, String fieldName,
                                  EntityMetadata entityMetadata, SpeedyEntity row, boolean unconditionalOnly) {
        boolean anyAllow = false;
        boolean anyDeny = false;
        for (SpeedyPolicy rule : document.speedyPolicies()) {
            if (!rule.getActions().contains(action)) {
                continue;
            }
            boolean resourceMatches = rule.getResources().stream().anyMatch(r ->
                    fieldName == null ? r.matchesEntity(entityName) : r.matches(entityName, fieldName));
            if (!resourceMatches) {
                continue;
            }

            boolean applies;
            if (rule.conditions().isEmpty()) {
                applies = true;
            } else if (unconditionalOnly) {
                applies = false;
            } else {
                ConditionContext ctx = new ConditionContext(row, variables, entityMetadata);
                applies = rule.conditions().stream().allMatch(c -> c.isSatisfied(ctx));
            }
            if (!applies) {
                continue;
            }

            if (rule.effect() == PolicyEffect.DENY) {
                anyDeny = true;
            } else {
                anyAllow = true;
            }
        }
        if (anyDeny) {
            return PolicyEffect.DENY;
        }
        if (anyAllow) {
            return PolicyEffect.ALLOW;
        }
        return document.defaultEffect();
    }
}
