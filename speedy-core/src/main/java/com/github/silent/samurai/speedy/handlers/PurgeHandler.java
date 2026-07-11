package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.ForbiddenException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.util.List;

/// Handles POST /{Entity}/$purge requests: permanently (hard) deletes rows by primary key,
/// bypassing soft delete.
///
/// Available only for soft-delete entities that explicitly opt in via
/// {@code @SpeedySoftDelete(allowHardDelete = true)} (secure by default). Each key must exist
/// (live or soft-deleted) else 404. Returns key-only. Fires no lifecycle events in this version.
///
/// @see AbstractKeyOperationHandler
/// @see com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor#purge
public class PurgeHandler extends AbstractKeyOperationHandler {

    @Override
    protected void preflight(EntityMetadata entityMetadata) throws SpeedyHttpException {
        if (!entityMetadata.isSoftDeleteEnabled()) {
            throw new BadRequestException(
                    "Purge is not supported for entity '" + entityMetadata.getName() + "' (soft delete not enabled)");
        }
        if (!entityMetadata.isHardDeleteAllowed()) {
            throw new ForbiddenException(
                    "Purge (permanent delete) is not allowed for entity '" + entityMetadata.getName() + "'");
        }
    }

    @Override
    protected List<SpeedyEntity> performKeys(SpeedyContext context, EntityMetadata entityMetadata,
                                             List<SpeedyEntityKey> keys) throws SpeedyHttpException {
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        return queryProcessor.purge(keys);
    }

    @Override
    protected SpeedyResponse buildSuccessResponse(EntityMetadata entityMetadata, List<SpeedyEntity> result) {
        return ResponseBuilders.keyOnlyResponse(entityMetadata, result);
    }

    @Override
    protected String operationLabel() {
        return "Purge";
    }
}
