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
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.PolicyTarget;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;

/// Applies policy decisions that do not need a persisted row for UPDATE, REPLACE, or DELETE.
///
/// The permission is supplied by the chain wiring, allowing UPDATE and REPLACE to reuse the same
/// implementation without request-type branching. Row-backed authorization is intentionally left
/// to each operation's existing-row stage.
public class WriteRequestPolicyHandler implements Handler {

    private final PermissionType permission;

    public WriteRequestPolicyHandler(PermissionType permission) {
        this.permission = permission;
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();

        enforceEntityAccess(engine, entityMetadata);
        enforceConditionFieldReadability(engine, entityMetadata);
    }

    private void enforceEntityAccess(PolicyEngine engine, EntityMetadata entityMetadata)
            throws ForbiddenException {
        if (engine.isAuthorized(permission, PolicyTarget.entity(entityMetadata)) == PolicyEffect.DENY) {
            throw new ForbiddenException(
                    String.format("%s not allowed for %s", permission.name().toLowerCase(), entityMetadata.getName()));
        }
    }

    private void enforceConditionFieldReadability(PolicyEngine engine, EntityMetadata entityMetadata)
            throws SpeedyHttpException {
        for (FieldMetadata field : engine.writeConditionFields(entityMetadata, permission)) {
            if (field instanceof KeyFieldMetadata) {
                continue;
            }
            if (engine.isAuthorized(PermissionType.READ, PolicyTarget.field(entityMetadata, field), null)
                    != PolicyEffect.ALLOW) {
                throw new ForbiddenException("Field '" + field.getOutputPropertyName()
                        + "' is used in a policy " + permission.name().toLowerCase()
                        + " condition but is not readable");
            }
        }
    }
}
