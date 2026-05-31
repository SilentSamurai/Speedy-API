package com.github.silent.samurai.speedy.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyPartialFailure;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.BatchResultSerializer;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@Slf4j
/// # DeleteHandler
///
/// Handles {@code DELETE /{Entity}/$delete} requests. Parses a JSON array of
/// primary keys, fires PRE/POST_DELETE events, validates, and bulk-deletes entities.
/// Supports both {@code BATCH} and {@code PER_ENTITY} transaction modes.
///
/// ## Purpose
/// - Parses a JSON array of PK objects into {@link SpeedyEntityKey} instances
/// - Verifies each entity exists before attempting deletion
/// - Supports two transaction modes: BATCH (all-or-nothing) and PER_ENTITY (partial success)
/// - Returns key-only projection for successful deletes, or a {@link BatchResultSerializer}
///   response with succeeded/failed arrays on partial failure
///
/// ## Processing Flow
/// 1. Parses the JSON array body into a list of {@code SpeedyEntityKey} objects
/// 2. Ensures each PK is complete before proceeding
/// 3. Branches by transaction mode:
///    - **BATCH**: Validates all keys exist, then deletes in a single transaction
///    - **PER_ENTITY**: Runs each delete in its own transaction; collects partial failures
/// 4. For each entity: validates → triggers PRE_DELETE event → deletes → triggers POST_DELETE
/// 5. On success: sets {@link JSONSerializerV2} with key-only projection
/// 6. On partial failure (PER_ENTITY with multiple keys): sets {@link BatchResultSerializer}
///
/// ## Chain Position
/// Dispatched by {@link SwitchHandler} for DELETE requests with {@code $delete} suffix.
public class DeleteHandler implements Handler {

    final Handler next;

    public DeleteHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        List<SpeedyEntityKey> keysToBeRemoved = parse(context);
        TransactionMode mode = context.getTransactionMode();

        if (mode == TransactionMode.BATCH) {
            processBatchDelete(context, keysToBeRemoved);
        } else {
            processPerEntityDelete(context, keysToBeRemoved);
        }

        next.process(context);
    }

    public List<SpeedyEntityKey> parse(RequestContext context) throws SpeedyHttpException {
        EntityMetadata resourceMetadata = context.getEntityMetadata();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        JsonNode jsonElement = context.getBody();

        List<SpeedyEntityKey> keysToBeRemoved = new LinkedList<>();

        if (jsonElement == null || !jsonElement.isArray()) {
            throw new BadRequestException("in-valid request");
        }
        ArrayNode batchOfEntities = (ArrayNode) jsonElement;
        for (JsonNode element : batchOfEntities) {
            if (element.isObject()) {
                if (!MetadataUtil.isPrimaryKeyComplete(resourceMetadata, (ObjectNode) element)) {
                    throw new BadRequestException("Primary Key Incomplete ");
                }
                SpeedyEntityKey pk = MetadataUtil.createIdentifierFromJSON(resourceMetadata, (ObjectNode) element);
                keysToBeRemoved.add(pk);
                log.info("parsed primary key {}", pk);
            } else {
                throw new BadRequestException("in-valid request body");
            }
        }
        return keysToBeRemoved;
    }

    private void processBatchDelete(RequestContext context, List<SpeedyEntityKey> keys)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        String entityLabel = entityMetadata.getName();
        int totalCount = keys.size();

        if (keys.isEmpty()) {
            context.setResponseSerializer(new JSONSerializerV2(
                    KeyFieldMetadata.class::isInstance,
                    List.of(), 0, new HashSet<>()));
            return;
        }

        try {
            queryProcessor.runInTransaction(() -> {
                try {
                    for (SpeedyEntityKey key : keys) {
                        if (!queryProcessor.exists(key)) {
                            throw new BadRequestException("entity not found");
                        }
                        context.getValidationProcessor().validateDeleteRequestEntity(entityMetadata, key);
                        eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE, entityMetadata, key);
                    }

                    List<SpeedyEntity> deleted = queryProcessor.delete(keys);

                    for (SpeedyEntityKey key : keys) {
                        eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE, entityMetadata, key);
                    }

                    context.setResponseSerializer(new JSONSerializerV2(
                            KeyFieldMetadata.class::isInstance, deleted, 0, new HashSet<>()));
                } catch (Exception ex) {
                    if (ex instanceof SpeedyHttpRuntimeException re) throw re;
                    if (ex instanceof RuntimeException re) throw re;
                    if (ex instanceof SpeedyHttpException she) {
                        throw new SpeedyHttpRuntimeException(she.getStatus(), she);
                    }
                    throw new SpeedyHttpRuntimeException(500, ex);
                }
            });

            log.info("BATCH delete committed: entity={}, count={}", entityLabel, totalCount);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.info("BATCH delete rolled back: entity={}, count={}", entityLabel, totalCount);
            if (cause instanceof SpeedyHttpException she) {
                throw she;
            }
            if (cause instanceof SpeedyHttpRuntimeException sre) {
                throw new SpeedyHttpException(sre.getStatus(), sre.getMessage(), sre);
            }
            throw new InternalServerError("Batch delete failed", e);
        }
    }

    private void processPerEntityDelete(RequestContext context, List<SpeedyEntityKey> keys)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        String entityLabel = entityMetadata.getName();

        List<SpeedyEntity> succeeded = new ArrayList<>();
        List<SpeedyPartialFailure> failed = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            SpeedyEntityKey key = keys.get(i);
            try {
                queryProcessor.runInTransaction(() -> {
                    try {
                        if (!queryProcessor.exists(key)) {
                            throw new BadRequestException("entity not found");
                        }
                        context.getValidationProcessor().validateDeleteRequestEntity(entityMetadata, key);
                        eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE, entityMetadata, key);

                        List<SpeedyEntity> singleResult = queryProcessor.delete(List.of(key));

                        eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE, entityMetadata, key);
                        if (!singleResult.isEmpty()) {
                            succeeded.add(singleResult.get(0));
                        }
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
                if (cause instanceof SpeedyHttpException she) {
                    failed.add(new SpeedyPartialFailure(i, she.getStatus(),
                            she.getMessage(), Instant.now().toString(),
                            key, she));
                    log.info("Entity #{} failed in per-entity transaction", i, she);
                } else if (cause instanceof SpeedyHttpRuntimeException sre) {
                    failed.add(new SpeedyPartialFailure(i, sre.getStatus(),
                            sre.getMessage(), Instant.now().toString(),
                            key, sre));
                    log.info("Entity #{} failed in per-entity transaction", i, sre);
                } else {
                    failed.add(new SpeedyPartialFailure(i, 500,
                            e.getMessage(), Instant.now().toString(),
                            key, e));
                    log.info("Entity #{} failed in per-entity transaction", i, e);
                }
            }
        }

        log.info("Transaction committed: entity={}, mode=PER_ENTITY, count={}, succeeded={}, failed={}",
                entityLabel, keys.size(), succeeded.size(), failed.size());

        IResponseSerializerV2 serializer;
        if (failed.isEmpty()) {
            serializer = new JSONSerializerV2(KeyFieldMetadata.class::isInstance,
                    succeeded, 0, new HashSet<>());
        } else if (keys.size() == 1 && !failed.isEmpty()) {
            SpeedyPartialFailure failure = failed.get(0);
            if (failure.getStatus() == 400) {
                throw new BadRequestException(failure.getMessage(), failure.getCause());
            }
            throw new InternalServerError(failure.getMessage(), failure.getCause());
        } else {
            serializer = new BatchResultSerializer(succeeded, failed, 0);
        }
        context.setResponseSerializer(serializer);
    }

}
