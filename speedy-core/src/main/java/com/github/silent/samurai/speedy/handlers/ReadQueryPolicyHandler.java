package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.ForbiddenException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.interfaces.query.OrderBy;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.PolicyTarget;
import com.github.silent.samurai.speedy.policy.condition.ConditionResolver;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.utils.ReadQueryResolver;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/// Applies every policy decision that can be made before a GET or $query is executed.
///
/// Both operations resolve to the same {@link SpeedyQuery} shape, so this handler performs their
/// shared entity gate, filter/sort field checks, row-visibility injection, and policy-variable
/// resolution without branching on the request type. Row-backed response field filtering remains
/// the responsibility of {@link ReadFieldFilterHandler} after the query has executed.
public class ReadQueryPolicyHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));
        SpeedyQuery query = ReadQueryResolver.resolve(context);
        EntityMetadata entityMetadata = query.getFrom();

        enforceEntityAccess(engine, entityMetadata);
        enforceReferencedFieldAccess(engine, query);
        injectRowVisibility(engine, entityMetadata, query);
        resolveVariables(engine, query);
    }

    private void enforceEntityAccess(PolicyEngine engine, EntityMetadata entityMetadata)
            throws ForbiddenException {
        if (engine.isAuthorized(PermissionType.READ, PolicyTarget.entity(entityMetadata)) == PolicyEffect.DENY) {
            throw new ForbiddenException("read not allowed for " + entityMetadata.getName());
        }
    }

    private void enforceReferencedFieldAccess(PolicyEngine engine, SpeedyQuery query)
            throws BadRequestException {
        Set<FieldMetadata> referenced = new LinkedHashSet<>();
        collectFields(query.getWhere(), referenced);
        for (OrderBy orderBy : query.getOrderByList()) {
            referenced.add(orderBy.getField().getMetadataForParsing());
        }

        for (FieldMetadata field : referenced) {
            PolicyTarget target = PolicyTarget.field(field.getEntityMetadata(), field);
            if (engine.isAuthorized(PermissionType.READ, target, null) != PolicyEffect.ALLOW) {
                throw new BadRequestException(
                        "Field '" + field.getOutputPropertyName() + "' cannot be used in a filter or sort");
            }
        }
    }

    private void injectRowVisibility(PolicyEngine engine, EntityMetadata entityMetadata, SpeedyQuery query) {
        for (Condition condition : engine.rowVisibilityConditions(entityMetadata)) {
            query.getWhere().addSubCondition(condition);
        }
    }

    private void resolveVariables(PolicyEngine engine, SpeedyQuery query) throws SpeedyHttpException {
        Map<String, SpeedyValue> variables = engine.getVariables();
        if (variables.isEmpty()) {
            return;
        }
        BooleanCondition where = query.getWhere();
        if (where == null || where.getConditions().isEmpty()) {
            return;
        }
        Condition resolved = ConditionResolver.resolve(where, variables);
        if (resolved != where) {
            ((SpeedyQueryImpl) query).setWhere((BooleanCondition) resolved);
        }
    }

    private void collectFields(Condition condition, Set<FieldMetadata> out) {
        if (condition == null) {
            return;
        }
        if (condition instanceof BooleanCondition booleanCondition) {
            for (Condition subCondition : booleanCondition.getConditions()) {
                collectFields(subCondition, out);
            }
        } else if (condition instanceof BinaryCondition binaryCondition) {
            out.add(binaryCondition.getField().getMetadataForParsing());
        }
    }
}
