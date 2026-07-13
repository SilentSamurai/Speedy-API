package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/// Handles POST /{Entity}/$create requests with pre-parsed SpeedyCreateBody.
///
/// Reads the SpeedyCreateBody (parsed from JSON by DefaultRequestParser and set as body by
/// CreateBodyParserHandler) and persists every entity in a single all-or-nothing transaction,
/// firing POST_INSERT for each saved row. PRE_INSERT events ({@link CreatePreEventHandler}) and
/// validation ({@link RequestValidationHandler}) run earlier in the chain, so a single failing
/// entity aborts the whole request before or during the transaction.
///
/// @see CreateBodyParserHandler
/// @see SpeedyCreateBody
@Slf4j
public class CreateHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyCreateBody body = (SpeedyCreateBody) context.get(SpeedyBody.class);
        List<SpeedyEntity> entities = body.getEntities();
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        String entityLabel = entityMetadata.getName();
        int totalCount = entities.size();

        if (entities.isEmpty()) {
            context.put(SpeedyResponse.class,
                    ResponseBuilders.emptyKeyOnlyResponse(entityMetadata));
            return;
        }

        try {
            queryProcessor.runInTransaction(TransactionRunner.wrap(() -> {
                    List<SpeedyEntity> saved = queryProcessor.create(entities);

                    for (SpeedyEntity entity : saved) {
                        if (entity == null || entity.isEmpty()) {
                            log.info("{} save failed", entityLabel);
                            continue;
                        }
                        log.info("{} saved {}", entityLabel, entity);
                        if (!MetadataUtil.isKeyCompleteInEntity(entityMetadata, entity)) {
                            throw new BadRequestException("Incomplete Key after save");
                        }
                        eventProcessor.triggerEvent(SpeedyEventType.POST_INSERT, entityMetadata, entity);
                    }

                    context.put(SpeedyResponse.class,
                            ResponseBuilders.keyOnlyResponse(entityMetadata, saved));
            }));
            log.info("CREATE committed: entity={}, count={}", entityLabel, totalCount);
        } catch (Exception e) {
            log.info("CREATE rolled back: entity={}, count={}", entityLabel, totalCount);
            throw TransactionRunner.unwrap("Create", e);
        }
    }
}
