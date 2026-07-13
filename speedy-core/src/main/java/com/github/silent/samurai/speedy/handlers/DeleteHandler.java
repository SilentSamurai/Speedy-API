package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/// Handles DELETE /{Entity}/$delete requests with pre-parsed SpeedyDeleteBody.
///
/// Reads the SpeedyDeleteBody (parsed from JSON by DefaultRequestParser and set as body by
/// DeleteBodyParserHandler) and deletes every key in a single all-or-nothing transaction, firing
/// POST_DELETE for each removed row. Existence ({@link ExistsInDbCheckHandler}), row-level DELETE
/// policy ({@link DeleteRowPolicyPreflightHandler}), PRE_DELETE events
/// ({@link DeletePreEventHandler}), and validation ({@link RequestValidationHandler}) all run
/// before this handler opens a transaction, so a single failing key aborts the whole request.
///
/// @see DeleteBodyParserHandler
/// @see SpeedyDeleteBody
@Slf4j
public class DeleteHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyDeleteBody body = (SpeedyDeleteBody) context.get(SpeedyBody.class);
        List<SpeedyEntityKey> keys = body.getKeys();
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        String entityLabel = entityMetadata.getName();
        int totalCount = keys.size();

        if (keys.isEmpty()) {
            context.put(SpeedyResponse.class,
                    ResponseBuilders.emptyKeyOnlyResponse(entityMetadata));
            return;
        }

        try {
            queryProcessor.runInTransaction(TransactionRunner.wrap(() -> {
                    List<SpeedyEntity> deleted = queryProcessor.delete(keys);

                    // POST-event carries the full deleted row (as it existed), not just the key.
                    for (SpeedyEntity entity : deleted) {
                        eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE, entityMetadata, entity);
                    }

                    context.put(SpeedyResponse.class,
                            ResponseBuilders.keyOnlyResponse(entityMetadata, deleted));
            }));

            log.info("DELETE committed: entity={}, count={}", entityLabel, totalCount);
        } catch (Exception e) {
            log.info("DELETE rolled back: entity={}, count={}", entityLabel, totalCount);
            throw TransactionRunner.unwrap("Delete", e);
        }
    }

}
