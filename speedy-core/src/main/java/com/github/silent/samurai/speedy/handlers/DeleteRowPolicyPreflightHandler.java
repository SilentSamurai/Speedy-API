package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.ForbiddenException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.PolicyTarget;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;

/// Verifies every DELETE target passes row-level DELETE policy before the transaction.
///
/// Row-backed authorization used to live inside {@link DeleteHandler}'s transaction, which meant a
/// denied delete only failed after the framework had opened (and had to roll back) a transaction.
/// Running the authorization here rejects the whole request up front, reusing the rows
/// {@link ExistsInDbCheckHandler} already fetched instead of hitting the database again. A policy
/// denial aborts the entire delete, matching the all-or-nothing write contract.
public class DeleteRowPolicyPreflightHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyDeleteBody body = (SpeedyDeleteBody) context.get(SpeedyBody.class);
        if (body.getKeys().isEmpty()) {
            return;
        }

        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        DbCheckEntities existingEntities = context.get(DbCheckEntities.class);
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));

        for (SpeedyEntityKey key : body.getKeys()) {
            SpeedyEntity row = existingEntities.get(key);
            if (engine.isAuthorized(PermissionType.DELETE, PolicyTarget.entity(entityMetadata), row)
                    == PolicyEffect.DENY) {
                throw new ForbiddenException("delete not allowed for " + entityMetadata.getName());
            }
        }
    }
}
