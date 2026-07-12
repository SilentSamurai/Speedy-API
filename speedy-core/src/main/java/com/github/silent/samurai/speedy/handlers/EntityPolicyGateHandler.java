package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.ForbiddenException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.PolicyTarget;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;

public class EntityPolicyGateHandler implements Handler {

    private final PermissionType permission;

    public EntityPolicyGateHandler(PermissionType permission) {
        this.permission = permission;
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));
        EntityMetadata entityMetadata = context.find(SpeedyUriContext.class)
                .orElseThrow(() -> new InternalServerError("URI context is required"))
                .getParsedQuery().getFrom();

        if (engine.isAuthorized(permission, PolicyTarget.entity(entityMetadata)) == PolicyEffect.DENY) {
            throw new ForbiddenException(
                    String.format("%s not allowed for %s", permission.name().toLowerCase(), entityMetadata.getName()));
        }
    }
}
