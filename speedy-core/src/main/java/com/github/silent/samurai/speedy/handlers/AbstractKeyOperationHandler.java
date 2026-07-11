package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.models.SpeedyBatchResponse;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyPartialFailure;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Shared scaffolding for the by-key list operations that read a {@link SpeedyDeleteBody} (a list
/// of primary keys + a {@link TransactionMode}): {@link DeleteHandler}, {@link RestoreHandler} and
/// {@link PurgeHandler}. Owns the BATCH (one shared transaction, all-or-nothing) vs PER_ENTITY
/// (per-key transaction, partial-failure with HTTP 207) execution — mirroring
/// {@link AbstractUpdateHandler} for the update/replace pair.
///
/// Subclasses supply only what differs: an optional {@link #preflight} gate, the per-key work
/// {@link #performKeys} (existence, events, and the actual mutation, run inside an already-open
/// transaction), and the all-succeeded {@link #buildSuccessResponse}.
@Slf4j
public abstract class AbstractKeyOperationHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyDeleteBody body = (SpeedyDeleteBody) context.get(SpeedyBody.class);
        List<SpeedyEntityKey> keys = body.getKeys();
        TransactionMode mode = body.getMode();
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();

        preflight(entityMetadata);

        if (keys.isEmpty()) {
            context.put(SpeedyResponse.class, buildSuccessResponse(entityMetadata, List.of()));
            return;
        }

        if (mode == TransactionMode.BATCH) {
            processBatch(context, entityMetadata, keys);
        } else {
            processPerEntity(context, entityMetadata, keys);
        }
    }

    /// Endpoint-level guards run before any work (e.g., restore/purge require soft delete). No-op by default.
    protected void preflight(EntityMetadata entityMetadata) throws SpeedyHttpException {
    }

    /// Performs the operation for {@code keys} inside an already-open transaction and returns the
    /// affected rows. Called with the full list (BATCH) or a single key (PER_ENTITY). Must throw a
    /// {@link SpeedyHttpException} (e.g. {@link com.github.silent.samurai.speedy.exceptions.NotFoundException})
    /// for a key it cannot process.
    protected abstract List<SpeedyEntity> performKeys(SpeedyContext context, EntityMetadata entityMetadata,
                                                      List<SpeedyEntityKey> keys) throws SpeedyHttpException;

    /// Builds the all-succeeded response (e.g. key-only for delete/purge, full rows for restore).
    protected abstract SpeedyResponse buildSuccessResponse(EntityMetadata entityMetadata, List<SpeedyEntity> result);

    /// Human-readable operation name used in transaction log lines (e.g. "Delete", "Restore", "Purge").
    protected abstract String operationLabel();

    private void processBatch(SpeedyContext context, EntityMetadata entityMetadata, List<SpeedyEntityKey> keys)
            throws SpeedyHttpException {
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        String entityLabel = entityMetadata.getName();
        int totalCount = keys.size();

        try {
            List<SpeedyEntity> result = new ArrayList<>();
            queryProcessor.runInTransaction(TransactionRunner.wrap(() ->
                    result.addAll(performKeys(context, entityMetadata, keys))));
            context.put(SpeedyResponse.class, buildSuccessResponse(entityMetadata, result));
            log.info("BATCH {} committed: entity={}, count={}", operationLabel(), entityLabel, totalCount);
        } catch (Exception e) {
            log.info("BATCH {} rolled back: entity={}, count={}", operationLabel(), entityLabel, totalCount);
            throw TransactionRunner.unwrap("Batch " + operationLabel(), e);
        }
    }

    private void processPerEntity(SpeedyContext context, EntityMetadata entityMetadata, List<SpeedyEntityKey> keys)
            throws SpeedyHttpException {
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        String entityLabel = entityMetadata.getName();

        List<SpeedyEntity> succeeded = new ArrayList<>();
        List<SpeedyPartialFailure> failed = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            SpeedyEntityKey key = keys.get(i);
            try {
                List<SpeedyEntity> result = new ArrayList<>();
                queryProcessor.runInTransaction(TransactionRunner.wrap(() ->
                        result.addAll(performKeys(context, entityMetadata, List.of(key)))));
                succeeded.addAll(result);
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

        log.info("Transaction committed: entity={}, mode=PER_ENTITY, op={}, count={}, succeeded={}, failed={}",
                entityLabel, operationLabel(), keys.size(), succeeded.size(), failed.size());

        if (failed.isEmpty()) {
            context.put(SpeedyResponse.class, buildSuccessResponse(entityMetadata, succeeded));
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
