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
import com.github.silent.samurai.speedy.policy.PolicyTarget;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;

/// Applies to CREATE policy to the entity and to the request's candidate field values.
///
/// No persisted row is involved in a creation, so coarse entity authorization and per-field
/// authorization belong to the same request-policy stage.
public class CreateRequestPolicyHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();

        if (engine.isAuthorized(PermissionType.CREATE, PolicyTarget.entity(entityMetadata)) == PolicyEffect.DENY) {
            throw new ForbiddenException("create not allowed for " + entityMetadata.getName());
        }

        SpeedyCreateBody body = (SpeedyCreateBody) context.get(SpeedyBody.class);
        for (SpeedyEntity entity : body.getEntities()) {
            enforceFieldAccess(engine, entityMetadata, entity);
        }
    }

    private void enforceFieldAccess(PolicyEngine engine, EntityMetadata entityMetadata, SpeedyEntity entity)
            throws ForbiddenException {
        for (FieldMetadata field : entityMetadata.getAllFields()) {
            if (!entity.has(field)) {
                continue;
            }
            if (engine.isAuthorized(PermissionType.CREATE, PolicyTarget.field(entityMetadata, field), entity)
                    != PolicyEffect.ALLOW) {
                throw new ForbiddenException(
                        "Field '" + field.getOutputPropertyName() + "' not permitted on create");
            }
        }
    }
}
