package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Handles DELETE /{Entity}/$delete requests with pre-parsed SpeedyDeleteBody.
///
/// Reads the SpeedyDeleteBody (parsed from JSON by DefaultRequestParser and set as
/// body by DeleteBodyParserHandler), fires PRE/POST_DELETE events, validates keys,
/// and bulk-deletes entities in either BATCH or PER_ENTITY transaction mode.
///
/// @see DeleteBodyParserHandler
/// @see SpeedyDeleteBody
@Slf4j
public class DeleteHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyDeleteBody body = (SpeedyDeleteBody) context.get(SpeedyBody.class);
        List<SpeedyEntityKey> keys = body.getKeys();
        TransactionMode mode = body.getMode();

        if (mode == TransactionMode.BATCH) {
            processBatchDelete(context, keys);
        } else {
            processPerEntityDelete(context, keys);
        }
    }

    private void processBatchDelete(SpeedyContext context, List<SpeedyEntityKey> keys)
            throws SpeedyHttpException {
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
                    // Batch existence check: one query instead of N
                    java.util.Set<SpeedyEntityKey> existingKeys = queryProcessor.findExistingKeys(keys);
                    for (SpeedyEntityKey key : keys) {
                        if (!existingKeys.contains(key)) {
                            throw new NotFoundException("entity not found: " + key);
                        }
                        context.get(ValidationProcessor.class).validateDeleteRequestEntity(entityMetadata, key);
                        eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE, entityMetadata, key);
                    }

                    List<SpeedyEntity> deleted = queryProcessor.delete(keys);

                    // POST event carries the full deleted row (as it existed), not just the key.
                    for (SpeedyEntity entity : deleted) {
                        eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE, entityMetadata, entity);
                    }

                    context.put(SpeedyResponse.class,
                            ResponseBuilders.keyOnlyResponse(entityMetadata, deleted));
            }));

            log.info("BATCH delete committed: entity={}, count={}", entityLabel, totalCount);
        } catch (Exception e) {
            log.info("BATCH delete rolled back: entity={}, count={}", entityLabel, totalCount);
            throw TransactionRunner.unwrap("Batch delete", e);
        }
    }

    private void processPerEntityDelete(SpeedyContext context, List<SpeedyEntityKey> keys)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        String entityLabel = entityMetadata.getName();

        List<SpeedyEntity> succeeded = new ArrayList<>();
        List<SpeedyPartialFailure> failed = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            SpeedyEntityKey key = keys.get(i);
            try {
                queryProcessor.runInTransaction(TransactionRunner.wrap(() -> {
                        if (!queryProcessor.exists(key)) {
                            throw new NotFoundException("entity not found: " + key);
                        }
                        context.get(ValidationProcessor.class).validateDeleteRequestEntity(entityMetadata, key);
                        eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE, entityMetadata, key);

                        List<SpeedyEntity> singleResult = queryProcessor.delete(List.of(key));

                        if (!singleResult.isEmpty()) {
                            // POST event carries the full deleted row (as it existed), not just the key.
                            eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE, entityMetadata, singleResult.get(0));
                            succeeded.add(singleResult.get(0));
                        }
                }));
            } catch (Exception e) {
                Throwable cause = TransactionRunner.extractCause(e);
                if (cause instanceof SpeedyHttpException she) {
                    failed.add(SpeedyPartialFailure.builder()
                            .index(i).status(she.getStatus())
                            .message(she.getMessage()).timestamp(Instant.now().toString())
                            .inputPk(key).cause(she).build());
                    log.info("Entity #{} failed in per-entity transaction", i, she);
                } else if (cause instanceof SpeedyHttpRuntimeException sre) {
                    failed.add(SpeedyPartialFailure.builder()
                            .index(i).status(sre.getStatus())
                            .message(sre.getMessage()).timestamp(Instant.now().toString())
                            .inputPk(key).cause(sre).build());
                    log.info("Entity #{} failed in per-entity transaction", i, sre);
                } else {
                    failed.add(SpeedyPartialFailure.builder()
                            .index(i).status(500)
                            .message(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                            .timestamp(Instant.now().toString())
                            .inputPk(key).cause(e).build());
                    log.info("Entity #{} failed in per-entity transaction", i, e);
                }
            }
        }

        log.info("Transaction committed: entity={}, mode=PER_ENTITY, count={}, succeeded={}, failed={}",
                entityLabel, keys.size(), succeeded.size(), failed.size());

        if (failed.isEmpty()) {
            context.put(SpeedyResponse.class,
                    ResponseBuilders.keyOnlyResponse(entityMetadata, succeeded));
        } else if (keys.size() == 1) {
            SpeedyPartialFailure failure = failed.get(0);
            throw new SpeedyHttpException(failure.getStatus(), failure.getMessage(), failure.getCause());
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
}
