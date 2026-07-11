package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/// Handles DELETE /{Entity}/$delete requests with a pre-parsed SpeedyDeleteBody.
///
/// Fires PRE/POST_DELETE events, validates keys, and (bulk-)deletes in either BATCH or PER_ENTITY
/// transaction mode. For a soft-delete entity the underlying {@code QueryProcessor.delete} sets the
/// marker instead of removing the row; the response payload carries the post-delete state.
///
/// @see AbstractKeyOperationHandler
/// @see DeleteBodyParserHandler
/// @see SpeedyDeleteBody
@Slf4j
public class DeleteHandler extends AbstractKeyOperationHandler {

    @Override
    protected List<SpeedyEntity> performKeys(SpeedyContext context, EntityMetadata entityMetadata,
                                             List<SpeedyEntityKey> keys) throws SpeedyHttpException {
        EventProcessor eventProcessor = context.get(EventProcessor.class);
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        ValidationProcessor validationProcessor = context.get(ValidationProcessor.class);

        // Batch existence check: one query for the whole key list (works for a single key too).
        Set<SpeedyEntityKey> existingKeys = queryProcessor.findExistingKeys(keys);
        for (SpeedyEntityKey key : keys) {
            if (!existingKeys.contains(key)) {
                throw new NotFoundException("entity not found: " + key);
            }
            validationProcessor.validateDeleteRequestEntity(entityMetadata, key);
            eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE, entityMetadata, key);
        }

        List<SpeedyEntity> deleted = queryProcessor.delete(keys);

        // POST event carries the full deleted row (as it existed), not just the key.
        for (SpeedyEntity entity : deleted) {
            eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE, entityMetadata, entity);
        }
        return deleted;
    }

    @Override
    protected SpeedyResponse buildSuccessResponse(EntityMetadata entityMetadata, List<SpeedyEntity> result) {
        return ResponseBuilders.keyOnlyResponse(entityMetadata, result);
    }

    @Override
    protected String operationLabel() {
        return "Delete";
    }
}
