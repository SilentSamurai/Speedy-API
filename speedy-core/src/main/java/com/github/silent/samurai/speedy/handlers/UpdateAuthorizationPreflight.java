package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.ForbiddenException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.PolicyTarget;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;

/// PATCH/PUT authorization gate. It runs in the update chain after ETag stamping and before the
/// terminal write handler, so every item is checked before any row lifecycle event or SQL write
/// starts, regardless of transaction mode.
public class UpdateAuthorizationPreflight implements Handler {

    private final PermissionType permission;

    public UpdateAuthorizationPreflight(PermissionType permission) {
        this.permission = permission;
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyUpdateBody body = (SpeedyUpdateBody) context.get(SpeedyBody.class);
        if (body.getItems().isEmpty()) {
            return;
        }

        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        DbCheckEntities existingEntities = context.get(DbCheckEntities.class);
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));

        for (SpeedyUpdateBody.Item item : body.getItems()) {
            enforceFieldPolicy(engine, entityMetadata, item.getEntity(), existingEntities.get(item.getPk()));
        }
    }

    private void enforceFieldPolicy(PolicyEngine engine, EntityMetadata entityMetadata,
                                    SpeedyEntity requestEntity, SpeedyEntity existingRow)
            throws SpeedyHttpException {
        for (FieldMetadata field : entityMetadata.getAllFields()) {
            if (field instanceof KeyFieldMetadata || !requestEntity.has(field)) {
                continue;
            }
            if (engine.isAuthorized(permission, PolicyTarget.field(entityMetadata, field), existingRow)
                    != PolicyEffect.ALLOW) {
                throw new ForbiddenException("Field '" + field.getOutputPropertyName() + "' not permitted on "
                        + permission.name().toLowerCase());
            }
        }
    }
}
