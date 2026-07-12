package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;
import com.github.silent.samurai.speedy.policy.condition.ConditionContext;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// The per-request decision core, built once from the {@link SpeedyAuthContext} a request resolved
/// (see {@code ISpeedyConfiguration.authContextPerReq()}). {@code DefaultSpeedyEngine} always
/// installs an engine; when the configuration supplies no context, it uses an empty
/// deny-by-default document.
///
/// Two ideas, not several:
/// - {@link #isAuthorized}: "is this (action, target, row) tuple allowed?" — two overloads. With a
///   row, this is the authoritative per-row/per-field decision (READ omission, CREATE/UPDATE field
///   rejection, DELETE). Without one, it answers what can be told before a row exists: for a
///   field-level target that's "is this field unconditionally readable" (a conditional Allow does
///   not grant, since there's no row to check its condition against — used to keep such fields out
///   of filters/sorts); for an entity-level target that's the coarse pre-flight gate "does this
///   caller have zero access to this entity whatsoever," deliberately asymmetric — an unconditional
///   whole-entity DENY blocks immediately, but a field-specific DENY does not, since other fields
///   may still be readable, while any ALLOW rule at all (even field-specific or conditional) is
///   enough to avoid the block and defer to the row-bearing overload once a row exists.
/// - {@link #rowVisibilityConditions}: not a per-target decision at all — it compiles a query
///   predicate for WHERE-injection so rows the caller can't read never come back from the
///   database. Fails open (empty list = don't restrict) where `isAuthorized` fails closed, because
///   per-row field omission remains the real security boundary regardless.
public class PolicyEngine {

    private final PolicyDocument document;
    @Getter
    private final Map<String, SpeedyValue> variables;

    public PolicyEngine(SpeedyAuthContext policy) {
        this.document = policy.document();
        this.variables = policy.variables() == null ? Map.of() : Map.copyOf(policy.variables());
    }

    /// Whether {@code action} is permitted on {@code target} given {@code row}'s current state.
    /// {@code target.field() == null} asks an entity-level question (CREATE/DELETE, or the
    /// READ/UPDATE entity gate's explicit-deny check); a non-null field asks a field-level question
    /// (READ omission, CREATE/UPDATE field rejection). {@code row == null} skips every conditional
    /// rule rather than evaluating it — used for query-time filter/sort checks, where a
    /// conditional Allow does not grant since there is no row to check its condition against
    /// Filtering/sorting on such a field is rejected rather than risk leaking information about
    /// rows the caller can't see an explicit DENY always beats an ALLOW; absent too, the
    /// document's default effect governs.
    public PolicyEffect isAuthorized(PermissionType action, PolicyTarget target, SpeedyEntity row) {
        String entityName = target.entity().getName();
        String fieldName = target.field() == null ? null : target.field().getOutputPropertyName();

        boolean anyAllow = false;
        boolean anyDeny = false;
        for (SpeedyPolicy rule : document.speedyPolicies()) {
            if (!rule.getActions().contains(action)) {
                continue;
            }
            if (!resourceMatches(rule, entityName, fieldName)) {
                continue;
            }

            boolean applies;
            if (rule.conditions().isEmpty()) {
                applies = true;
            } else if (row == null) {
                applies = false;
            } else {
                ConditionContext ctx = new ConditionContext(row, variables, target.entity());
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

    /// The no-row overload. For a field-level target this is the query-time "unconditionally
    /// readable" check, identical to calling the row-bearing overload with {@code row = null}. For
    /// an entity-level target this is the pre-flight "zero access at all" gate: an unconditional
    /// whole-entity DENY blocks immediately because it overrides every field grant; otherwise, a
    /// deny-by-default policy is only overcome by having at least one Allow rule for the entity —
    /// conditional grants included, since they may still grant access once a row is available.
    public PolicyEffect isAuthorized(PermissionType action, PolicyTarget target) {
        if (target.field() != null) {
            return isAuthorized(action, target, null);
        }
        String entityName = target.entity().getName();
        boolean anyAllow = false;
        for (SpeedyPolicy rule : document.speedyPolicies()) {
            if (!rule.getActions().contains(action)) {
                continue;
            }
            if (rule.effect() == PolicyEffect.DENY && rule.conditions().isEmpty()
                    && hasWholeEntityResourceMatch(rule, entityName)) {
                return PolicyEffect.DENY;
            }
            if (rule.effect() == PolicyEffect.ALLOW && resourceMatches(rule, entityName, null)) {
                anyAllow = true;
            }
        }
        return anyAllow ? PolicyEffect.ALLOW : document.defaultEffect();
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
            if (resourceMatches(rule, entityName, null)) {
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
            for (var condition : rule.conditions()) {
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

    /// Whether any of {@code rule}'s resource selectors reference this (entity, field) pair.
    /// {@code fieldName == null} matches any selector for the entity, whole-entity or
    /// field-specific alike (used for entity-level questions); a non-null {@code fieldName}
    /// requires a selector that actually covers that field.
    private static boolean resourceMatches(SpeedyPolicy rule, String entityName, String fieldName) {
        return rule.getResources().stream().anyMatch(selector ->
                fieldName == null ? selectorMatchesEntity(selector, entityName) : selectorMatches(selector, entityName, fieldName));
    }

    /// Whether {@code rule} has a selector that is specifically whole-entity ({@code Entity} or
    /// {@code Entity.*}), as opposed to merely referencing the entity via a field-specific selector.
    private static boolean hasWholeEntityResourceMatch(SpeedyPolicy rule, String entityName) {
        return rule.getResources().stream().anyMatch(selector ->
                selectorMatchesEntity(selector, entityName) && selectorIsWholeEntity(selector));
    }

    /// A selector's entity is everything before the first {@code '.'} (or the whole token if
    /// there is none).
    private static boolean selectorMatchesEntity(String selector, String entityName) {
        int dot = selector.indexOf('.');
        String entity = dot < 0 ? selector : selector.substring(0, dot);
        return entity.equals(entityName);
    }

    /// {@code "Entity"} and {@code "Entity.*"} cover every field; {@code "Entity.field"} covers
    /// only that field.
    private static boolean selectorMatches(String selector, String entityName, String fieldName) {
        if (!selectorMatchesEntity(selector, entityName)) {
            return false;
        }
        return selectorIsWholeEntity(selector) || selector.substring(selector.indexOf('.') + 1).equals(fieldName);
    }

    private static boolean selectorIsWholeEntity(String selector) {
        int dot = selector.indexOf('.');
        return dot < 0 || "*".equals(selector.substring(dot + 1));
    }
}
