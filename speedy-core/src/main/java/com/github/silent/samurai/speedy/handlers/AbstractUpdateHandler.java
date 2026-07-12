package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.ForbiddenException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.models.SpeedyBatchResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyPartialFailure;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.PolicyTarget;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/// Shared scaffolding for the two by-PK write operations, PATCH ({@link UpdateHandler}) and PUT
/// ({@link ReplaceHandler}). Both read a pre-parsed {@link SpeedyUpdateBody} (one or more
/// (entity, pk) items) and write them in either BATCH (one shared transaction, all-or-nothing) or
/// PER_ENTITY (per-item transaction, partial-failure) mode — mirroring {@link CreateHandler}/
/// {@link DeleteHandler}. Subclasses differ only in how each entity is validated and persisted,
/// supplied via {@link #validate} and {@link #persist}.
///
/// @see UpdateHandler
/// @see ReplaceHandler
@Slf4j
public abstract class AbstractUpdateHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyUpdateBody body = (SpeedyUpdateBody) context.get(SpeedyBody.class);
        List<SpeedyUpdateBody.Item> items = body.getItems();
        TransactionMode mode = body.getMode();
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();

        if (items.isEmpty()) {
            context.put(SpeedyResponse.class,
                    ResponseBuilders.updatedEntitiesResponse(entityMetadata, List.of()));
            return;
        }

        if (mode == TransactionMode.BATCH) {
            processBatchUpdate(context, items);
        } else {
            processPerEntityUpdate(context, items);
        }
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

    private void processBatchUpdate(SpeedyContext context, List<SpeedyUpdateBody.Item> items)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        String entityLabel = entityMetadata.getName();
        int totalCount = items.size();

        try {
            List<SpeedyEntity> saved = new ArrayList<>();
            queryProcessor.runInTransaction(TransactionRunner.wrap(() -> {
                    // Batch existence check: one query instead of N
                    List<SpeedyEntityKey> pks = new ArrayList<>(items.size());
                    for (SpeedyUpdateBody.Item item : items) {
                        pks.add(item.getPk());
                    }
                    Set<SpeedyEntityKey> existingKeys = queryProcessor.findExistingKeys(pks);
                    for (SpeedyUpdateBody.Item item : items) {
                        saved.add(writeOne(context, item.getEntity(), item.getPk(), existingKeys));
                    }
                    context.put(SpeedyResponse.class,
                            ResponseBuilders.updatedEntitiesResponse(entityMetadata, saved));
            }));

            log.info("BATCH {} committed: entity={}, count={}", operationLabel(), entityLabel, totalCount);
        } catch (Exception e) {
            log.info("BATCH {} rolled back: entity={}, count={}", operationLabel(), entityLabel, totalCount);
            throw TransactionRunner.unwrap("Batch " + operationLabel(), e);
        }
    }

    private void processPerEntityUpdate(SpeedyContext context, List<SpeedyUpdateBody.Item> items)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        String entityLabel = entityMetadata.getName();

        List<SpeedyEntity> succeeded = new ArrayList<>();
        List<SpeedyPartialFailure> failed = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            SpeedyUpdateBody.Item item = items.get(i);
            try {
                SpeedyEntity[] result = new SpeedyEntity[1];
                queryProcessor.runInTransaction(TransactionRunner.wrap(() -> {
                        result[0] = writeOne(context, item.getEntity(), item.getPk(), null);
                }));
                succeeded.add(result[0]);
            } catch (Exception e) {
                Throwable cause = TransactionRunner.extractCause(e);
                SpeedyEntityKey inputPk = item.getPk();
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
                            .message(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                            .timestamp(Instant.now().toString())
                            .inputPk(inputPk).cause(e).build());
                    log.info("Entity #{} failed in per-entity transaction", i, e);
                }
            }
        }

        log.info("Transaction committed: entity={}, mode=PER_ENTITY, count={}, succeeded={}, failed={}",
                entityLabel, items.size(), succeeded.size(), failed.size());

        if (failed.isEmpty()) {
            context.put(SpeedyResponse.class,
                    ResponseBuilders.updatedEntitiesResponse(entityMetadata, succeeded));
        } else if (items.size() == 1) {
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

    /// Writes a single (entity, pk) item: existence check, PRE_UPDATE event, validate, persist,
    /// POST_UPDATE event. Does not open a transaction itself — the caller (batch or per-entity)
    /// owns the transaction boundary.
    ///
    /// @param existingKeys pre-computed result of a batch {@link QueryProcessor#findExistingKeys}
    ///                      call (BATCH mode, one query for the whole item list); or {@code null}
    ///                      to fall back to a per-item {@link QueryProcessor#exists} check
    ///                      (PER_ENTITY mode, where each item already pays its own transaction).
    private SpeedyEntity writeOne(SpeedyContext context, SpeedyEntity entity, SpeedyEntityKey pk,
                                  Set<SpeedyEntityKey> existingKeys) throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        ValidationProcessor validationProcessor = context.get(ValidationProcessor.class);
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));

        // Distinguish a well-formed request whose target row is absent (404) from a validation
        // failure (400) by checking existence before validating — mirrors DeleteHandler. When a
        // policy is active, row-scoped UPDATE conditions (e.g. "only your own records") need the
        // row itself, not just an existence flag, so fetch it once, inside this transaction
        // (TOCTOU-safe), and reuse it for both purposes rather than paying for a second round-trip.
        Optional<SpeedyEntity> existingRow = queryProcessor.fetchByKey(pk);
        if (existingRow.isEmpty()) {
            throw new NotFoundException("entity not found: " + pk);
        }
        enforceUpdateFieldPolicy(engine, entityMetadata, entity, existingRow.get());

        eventProcessor.triggerEvent(SpeedyEventType.PRE_UPDATE, entityMetadata, entity);
        validate(validationProcessor, entityMetadata, entity);
        SpeedyEntity saved = persist(queryProcessor, pk, entity);
        if (saved == null) {
            throw new NotFoundException();
        }
        // POST event carries the persisted state (generated keys, DB defaults, untouched
        // columns), not the request payload — consistent with POST_INSERT.
        eventProcessor.triggerEvent(SpeedyEventType.POST_UPDATE, entityMetadata, saved);
        return saved;
    }

    /// Requirements 9/12 + row-level ABAC for UPDATE: a client-supplied mutable field the policy
    /// denies — evaluated against the row's *current* state, so "only your own records" style
    /// conditions work — fails the request explicitly. Key fields identify the target row and are
    /// never part of the update set, so they are intentionally excluded from this field check.
    private void enforceUpdateFieldPolicy(PolicyEngine engine, EntityMetadata entityMetadata,
                                          SpeedyEntity entity, SpeedyEntity existingRow) throws SpeedyHttpException {
        for (FieldMetadata field : entityMetadata.getAllFields()) {
            if (field instanceof KeyFieldMetadata || !entity.has(field)) {
                continue;
            }
            if (engine.isAuthorized(PermissionType.UPDATE, PolicyTarget.field(entityMetadata, field), existingRow)
                    != PolicyEffect.ALLOW) {
                throw new ForbiddenException("Field '" + field.getOutputPropertyName() + "' not permitted on update");
            }
        }
    }
}
