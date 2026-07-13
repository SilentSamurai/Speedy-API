package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/// Shared terminal write handler for PATCH ({@link UpdateHandler}) and PUT ({@link ReplaceHandler}).
/// Authorization ({@link UpdateAuthorizationPreflight}), PRE_UPDATE events
/// ({@link UpdatePreEventHandler}), and validation ({@link RequestValidationHandler}) all run earlier
/// in the engine chain; this handler persists every item in a single all-or-nothing transaction and
/// fires POST_UPDATE for each saved row. A single failing item aborts the whole request.
@Slf4j
public abstract class AbstractUpdateHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyUpdateBody body = (SpeedyUpdateBody) context.get(SpeedyBody.class);
        List<SpeedyUpdateBody.Item> items = body.getItems();
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        String entityLabel = entityMetadata.getName();
        int totalCount = items.size();

        if (items.isEmpty()) {
            context.put(SpeedyResponse.class,
                    ResponseBuilders.updatedEntitiesResponse(entityMetadata, List.of()));
            return;
        }

        try {
            List<SpeedyEntity> saved = new ArrayList<>();
            queryProcessor.runInTransaction(TransactionRunner.wrap(() -> {
                    for (SpeedyUpdateBody.Item item : items) {
                        saved.add(writeOne(context, item.getEntity(), item.getPk()));
                    }
                    context.put(SpeedyResponse.class,
                            ResponseBuilders.updatedEntitiesResponse(entityMetadata, saved));
            }));

            log.info("{} committed: entity={}, count={}", operationLabel(), entityLabel, totalCount);
        } catch (Exception e) {
            log.info("{} rolled back: entity={}, count={}", operationLabel(), entityLabel, totalCount);
            throw TransactionRunner.unwrap(operationLabel(), e);
        }
    }

    /// Persists the request entity under this write mode and returns the saved row.
    protected abstract SpeedyEntity persist(QueryProcessor queryProcessor,
                                            SpeedyEntityKey pk,
                                            SpeedyEntity entity) throws SpeedyHttpException;

    /// Human-readable operation name used in transaction log lines (e.g. "Update", "Replace").
    protected abstract String operationLabel();

    /// Runs the persistence and POST_UPDATE lifecycle for one already-authorized, already-validated row.
    private SpeedyEntity writeOne(SpeedyContext context, SpeedyEntity entity, SpeedyEntityKey pk)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);

        SpeedyEntity saved = persist(queryProcessor, pk, entity);
        if (saved == null) {
            throw new NotFoundException();
        }
        eventProcessor.triggerEvent(SpeedyEventType.POST_UPDATE, entityMetadata, saved);
        return saved;
    }

}
