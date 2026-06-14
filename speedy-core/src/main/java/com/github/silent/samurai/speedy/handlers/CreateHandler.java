package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Handles POST /{Entity}/$create requests with pre-parsed SpeedyCreateBody.
///
/// Reads the SpeedyCreateBody (parsed from JSON by JSONBodyParser and set as
/// body by CreateBodyParserHandler), fires PRE/POST_INSERT events, validates entities,
/// and bulk-creates them in either BATCH or PER_ENTITY transaction mode.
///
/// @see CreateBodyParserHandler
/// @see SpeedyCreateBody
@Slf4j
public class CreateHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyCreateBody body = (SpeedyCreateBody) context.get(SpeedyBody.class);
        List<SpeedyEntity> entities = body.getEntities();
        TransactionMode mode = body.getMode();

        if (mode == TransactionMode.BATCH) {
            processBatchCreate(context, entities);
        } else {
            processPerEntityCreate(context, entities);
        }
    }

    private void processBatchCreate(SpeedyContext context, List<SpeedyEntity> entities)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        String entityLabel = entityMetadata.getName();
        int totalCount = entities.size();

        if (entities.isEmpty()) {
            context.put(SpeedyResponse.class,
                    SpeedyEntityResponse.builder()
                            .entityMetadata(context.get(SpeedyUriContext.class).getParsedQuery().getFrom())
                            .payload(List.of())
                            .pageIndex(0)
                            .fieldPredicate(KeyFieldMetadata.class::isInstance)
                            .status(200)
                            .build()
            );
            return;
        }

        try {
            queryProcessor.runInTransaction(() -> {
                try {
                    for (SpeedyEntity entity : entities) {
                        eventProcessor.triggerEvent(SpeedyEventType.PRE_INSERT, entityMetadata, entity);
                        context.get(ValidationProcessor.class).validateCreateRequestEntity(entityMetadata, entity);
                    }

                    List<SpeedyEntity> saved = queryProcessor.create(entities);

                    for (SpeedyEntity entity : saved) {
                        if (entity == null || entity.isEmpty()) {
                            log.info("{} save failed", entityLabel);
                        } else {
                            log.info("{} saved {}", entityLabel, entity);
                        }
                        if (!MetadataUtil.isKeyCompleteInEntity(entityMetadata, entity)) {
                            throw new BadRequestException("Incomplete Key after save");
                        }
                        eventProcessor.triggerEvent(SpeedyEventType.POST_INSERT, entityMetadata, entity);
                    }

                    context.put(SpeedyResponse.class,
                            SpeedyEntityResponse.builder()
                                    .entityMetadata(context.get(SpeedyUriContext.class).getParsedQuery().getFrom())
                                    .payload(saved)
                                    .pageIndex(0)
                                    .fieldPredicate(KeyFieldMetadata.class::isInstance)
                                    .status(200)
                                    .build()
                    );
                } catch (Exception ex) {
                    if (ex instanceof SpeedyHttpRuntimeException re) throw re;
                    if (ex instanceof RuntimeException re) throw re;
                    if (ex instanceof SpeedyHttpException she) {
                        throw new SpeedyHttpRuntimeException(she.getStatus(), she);
                    }
                    throw new SpeedyHttpRuntimeException(500, ex);
                }
            });
        } catch (Exception e) {
            log.info("BATCH rolled back: entity={}, count={}", entityLabel, totalCount);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof SpeedyHttpException she) {
                throw she;
            }
            if (cause instanceof SpeedyHttpRuntimeException sre) {
                throw new SpeedyHttpException(sre.getStatus(), sre.getMessage(), sre);
            }
            throw new InternalServerError("Batch create failed", e);
        }
    }

    private void processPerEntityCreate(SpeedyContext context, List<SpeedyEntity> entities)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        String entityLabel = entityMetadata.getName();

        List<SpeedyEntity> succeeded = new ArrayList<>();
        List<SpeedyPartialFailure> failed = new ArrayList<>();

        for (int i = 0; i < entities.size(); i++) {
            SpeedyEntity entity = entities.get(i);
            try {
                queryProcessor.runInTransaction(() -> {
                    try {
                        eventProcessor.triggerEvent(SpeedyEventType.PRE_INSERT, entityMetadata, entity);
                        context.get(ValidationProcessor.class).validateCreateRequestEntity(entityMetadata, entity);

                        List<SpeedyEntity> singleBatch = List.of(entity);
                        List<SpeedyEntity> saved = queryProcessor.create(singleBatch);
                        SpeedyEntity savedEntity = saved.get(0);

                        if (savedEntity == null || savedEntity.isEmpty()) {
                            log.info("{} save failed", entityLabel);
                        } else {
                            log.info("{} saved {}", entityLabel, savedEntity);
                        }
                        if (!MetadataUtil.isKeyCompleteInEntity(entityMetadata, savedEntity)) {
                            throw new BadRequestException("Incomplete Key after save");
                        }
                        eventProcessor.triggerEvent(SpeedyEventType.POST_INSERT, entityMetadata, savedEntity);
                        succeeded.add(savedEntity);
                    } catch (Exception ex) {
                        if (ex instanceof SpeedyHttpRuntimeException re) throw re;
                        if (ex instanceof RuntimeException re) throw re;
                        if (ex instanceof SpeedyHttpException she) {
                            throw new SpeedyHttpRuntimeException(she.getStatus(), she);
                        }
                        throw new SpeedyHttpRuntimeException(500, ex);
                    }
                });
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                SpeedyEntityKey inputPk = extractInputPk(entity);
                if (cause instanceof SpeedyHttpException she) {
                    failed.add(SpeedyPartialFailure.builder()
                            .index(i).status(she.getStatus())
                            .message(she.getMessage()).timestamp(Instant.now().toString())
                            .inputPk(inputPk).cause(she).build());
                    log.info("Entity #{} failed in per-entity transaction", i, she);
                } else if (cause instanceof SpeedyHttpRuntimeException sre) {
                    failed.add(SpeedyPartialFailure.builder()
                            .index(i).status(sre.getStatus())
                            .message(sre.getMessage()).timestamp(Instant.now().toString())
                            .inputPk(inputPk).cause(sre).build());
                    log.info("Entity #{} failed in per-entity transaction", i, sre);
                } else {
                    failed.add(SpeedyPartialFailure.builder()
                            .index(i).status(500)
                            .message(e.getMessage()).timestamp(Instant.now().toString())
                            .inputPk(inputPk).cause(e).build());
                    log.info("Entity #{} failed in per-entity transaction", i, e);
                }
            }
        }

        log.info("Transaction committed: entity={}, mode=PER_ENTITY, count={}, succeeded={}, failed={}",
                entityLabel, entities.size(), succeeded.size(), failed.size());

                if (failed.isEmpty()) {
            context.put(SpeedyResponse.class,
                    SpeedyEntityResponse.builder()
                            .entityMetadata(context.get(SpeedyUriContext.class).getParsedQuery().getFrom())
                            .payload(succeeded)
                            .pageIndex(0)
                            .fieldPredicate(KeyFieldMetadata.class::isInstance)
                            .status(200)
                            .build()
            );
        } else if (entities.size() == 1) {
            SpeedyPartialFailure failure = failed.get(0);
            if (failure.getStatus() == 400) {
                throw new BadRequestException(failure.getMessage(), failure.getCause());
            }
            throw new InternalServerError(failure.getMessage(), failure.getCause());
        } else {
            int status = succeeded.isEmpty() ? 400 : 207;
            context.put(SpeedyResponse.class,
                    SpeedyBatchResponse.builder()
                            .succeeded(succeeded)
                            .failed(failed)
                            .pageIndex(0)
                            .status(status)
                            .build()
            );
        }
    }

    private SpeedyEntityKey extractInputPk(SpeedyEntity entity) {
        EntityMetadata metadata = entity.getMetadata();
        SpeedyEntityKey key = new SpeedyEntityKey(metadata);
        boolean hasAtLeastOne = false;
        for (KeyFieldMetadata keyField : metadata.getKeyFields()) {
            if (entity.has(keyField) && !entity.get(keyField).isNull()
                    && !entity.get(keyField).isEmpty()) {
                key.put(keyField, entity.get(keyField));
                hasAtLeastOne = true;
            }
        }
        return hasAtLeastOne ? key : null;
    }
}
