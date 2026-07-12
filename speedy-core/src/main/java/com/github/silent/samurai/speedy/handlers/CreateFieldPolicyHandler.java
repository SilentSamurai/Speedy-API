package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.ForbiddenException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyCreateBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.policy.PolicyEngine;

public class CreateFieldPolicyHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));
        SpeedyCreateBody body = (SpeedyCreateBody) context.get(SpeedyBody.class);
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();

        for (SpeedyEntity entity : body.getEntities()) {
            for (FieldMetadata field : entityMetadata.getAllFields()) {
                if (!entity.has(field)) {
                    continue;
                }
                if (!engine.isFieldAllowed(PermissionType.CREATE, entityMetadata, field, entity)) {
                    throw new ForbiddenException(
                            "Field '" + field.getOutputPropertyName() + "' not permitted on create");
                }
            }
        }
    }
}
