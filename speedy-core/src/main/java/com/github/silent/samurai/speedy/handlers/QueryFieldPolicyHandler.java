package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.interfaces.query.OrderBy;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.PolicyTarget;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.utils.ReadQueryResolver;

import java.util.LinkedHashSet;
import java.util.Set;

public class QueryFieldPolicyHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));
        SpeedyQuery query = ReadQueryResolver.resolve(context);

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

    private void collectFields(Condition condition, Set<FieldMetadata> out) {
        if (condition == null) {
            return;
        }
        if (condition instanceof BooleanCondition bc) {
            for (Condition sub : bc.getConditions()) {
                collectFields(sub, out);
            }
        } else if (condition instanceof BinaryCondition bc) {
            out.add(bc.getField().getMetadataForParsing());
        }
    }
}
