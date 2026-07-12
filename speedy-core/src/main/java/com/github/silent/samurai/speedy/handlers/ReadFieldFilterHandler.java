package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.PolicyTarget;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;

import java.util.function.BiPredicate;

public class ReadFieldFilterHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyResponse response = context.get(SpeedyResponse.class);
        if (!(response instanceof SpeedyEntityResponse entityResponse)) {
            return;
        }
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));

        BiPredicate<SpeedyEntity, FieldMetadata> existing = entityResponse.getFieldPredicate();
        BiPredicate<SpeedyEntity, FieldMetadata> policyVisible = (row, field) ->
                field instanceof KeyFieldMetadata
                        || engine.isAuthorized(PermissionType.READ, PolicyTarget.field(row.getMetadata(), field), row)
                                == PolicyEffect.ALLOW;

        context.put(SpeedyResponse.class, entityResponse.toBuilder()
                .fieldPredicate(existing.and(policyVisible))
                .build());
    }
}
