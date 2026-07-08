package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/// Shared scaffolding for the two single-entity-by-PK write operations, PATCH ({@link UpdateHandler})
/// and PUT ({@link ReplaceHandler}). Both read a pre-parsed {@link SpeedyUpdateBody}, fire
/// PRE/POST_UPDATE events, run in one transaction, and build the same 200 response — they differ
/// only in how the entity is validated and persisted, which subclasses supply via {@link #validate}
/// and {@link #persist}.
///
/// @see UpdateHandler
/// @see ReplaceHandler
@Slf4j
public abstract class AbstractUpdateHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyUpdateBody body = (SpeedyUpdateBody) context.get(SpeedyBody.class);
        SpeedyEntity savedEntity = writeInTransaction(context, body.getEntity(), body.getPk());

        if (savedEntity == null) {
            throw new NotFoundException();
        }

        List<SpeedyEntity> speedyEntities = List.of(savedEntity);
        context.put(SpeedyResponse.class,
                SpeedyEntityResponse.builder()
                        .entityMetadata(context.get(SpeedyUriContext.class).getParsedQuery().getFrom())
                        .payload(speedyEntities)
                        .pageIndex(0)
                        .status(200)
                        .build()
        );
    }

    /// Validates the request entity under this write mode (partial vs full-replace).
    protected abstract void validate(ValidationProcessor validationProcessor,
                                     EntityMetadata entityMetadata,
                                     SpeedyEntity entity) throws SpeedyHttpException;

    /// Persists the request entity under this write mode and returns the saved row.
    protected abstract SpeedyEntity persist(QueryProcessor queryProcessor,
                                            SpeedyEntityKey pk,
                                            SpeedyEntity entity) throws SpeedyHttpException;

    /// Human-readable operation name used in transaction log lines (e.g. "Update", "Replace").
    protected abstract String operationLabel();

    private SpeedyEntity writeInTransaction(SpeedyContext context, SpeedyEntity entity, SpeedyEntityKey pk)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        ValidationProcessor validationProcessor = context.get(ValidationProcessor.class);
        TransactionMode mode = context.get(TransactionMode.class);
        String entityLabel = entityMetadata.getName();

        try {
            SpeedyEntity[] result = new SpeedyEntity[1];
            queryProcessor.runInTransaction(TransactionRunner.wrap(() -> {
                    eventProcessor.triggerEvent(SpeedyEventType.PRE_UPDATE, entityMetadata, entity);
                    validate(validationProcessor, entityMetadata, entity);
                    result[0] = persist(queryProcessor, pk, entity);
                    // POST event carries the persisted state (generated keys, DB defaults, untouched
                    // columns), not the request payload — consistent with POST_INSERT.
                    eventProcessor.triggerEvent(SpeedyEventType.POST_UPDATE, entityMetadata, result[0]);
            }));

            log.info("{} committed: entity={}, mode={}, pk={}", operationLabel(), entityLabel, mode, pk);
            return result[0];
        } catch (Exception e) {
            log.info("{} rolled back: entity={}, mode={}, pk={}", operationLabel(), entityLabel, mode, pk);
            throw TransactionRunner.unwrap(operationLabel(), e);
        }
    }
}
