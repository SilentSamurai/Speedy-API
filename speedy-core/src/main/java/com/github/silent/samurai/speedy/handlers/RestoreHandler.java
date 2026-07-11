package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.util.List;

/// Handles POST /{Entity}/$restore requests: un-deletes soft-deleted rows by clearing the marker.
///
/// Available only for soft-delete entities. Each key must identify a currently soft-deleted row
/// (else 404). Returns the restored (now live) rows. Fires no lifecycle events in this version.
///
/// @see AbstractKeyOperationHandler
/// @see com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor#restore
public class RestoreHandler extends AbstractKeyOperationHandler {

    @Override
    protected void preflight(EntityMetadata entityMetadata) throws SpeedyHttpException {
        if (!entityMetadata.isSoftDeleteEnabled()) {
            throw new BadRequestException(
                    "Restore is not supported for entity '" + entityMetadata.getName() + "' (soft delete not enabled)");
        }
    }

    @Override
    protected List<SpeedyEntity> performKeys(SpeedyContext context, EntityMetadata entityMetadata,
                                             List<SpeedyEntityKey> keys) throws SpeedyHttpException {
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        return queryProcessor.restore(keys);
    }

    @Override
    protected SpeedyResponse buildSuccessResponse(EntityMetadata entityMetadata, List<SpeedyEntity> result) {
        // Return the restored rows in full — the row is live again, not just a key.
        return ResponseBuilders.updatedEntitiesResponse(entityMetadata, result);
    }

    @Override
    protected String operationLabel() {
        return "Restore";
    }
}
