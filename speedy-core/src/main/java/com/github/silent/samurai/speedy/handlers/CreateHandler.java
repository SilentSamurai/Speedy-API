package com.github.silent.samurai.speedy.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class CreateHandler implements Handler {

    final Handler next;

    public CreateHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        EntityMetadata resourceMetadata = context.getEntityMetadata();
        ObjectMapper json = CommonUtil.json();
        JsonNode jsonBody = context.getBody();

        List<SpeedyEntity> speedyEntities = parserContent(context, resourceMetadata, jsonBody);

        TransactionMode mode = context.getTransactionMode();
        if (mode == TransactionMode.BATCH) {
            processBatchCreate(context, speedyEntities);
        } else {
            processPerEntityCreate(context, speedyEntities);
        }

        next.process(context);
    }

    private void processBatchCreate(RequestContext context, List<SpeedyEntity> entities)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        String entityLabel = entityMetadata.getName();
        int totalCount = entities.size();

        if (entities.isEmpty()) {
            context.setResponseSerializer(new JSONSerializerV2(
                    KeyFieldMetadata.class::isInstance,
                    List.of(), 0, new HashSet<>()));
            return;
        }

        try {
            queryProcessor.runInTransaction(() -> {
                try {
                    for (SpeedyEntity entity : entities) {
                        eventProcessor.triggerEvent(SpeedyEventType.PRE_INSERT, entityMetadata, entity);
                        context.getValidationProcessor().validateCreateRequestEntity(entityMetadata, entity);
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

                    context.setResponseSerializer(new JSONSerializerV2(
                            KeyFieldMetadata.class::isInstance,
                            saved, 0, new HashSet<>()));
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

    private void processPerEntityCreate(RequestContext context, List<SpeedyEntity> entities)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        String entityLabel = entityMetadata.getName();

        List<SpeedyEntity> succeeded = new ArrayList<>();
        List<SpeedyPartialFailure> failed = new ArrayList<>();

        for (int i = 0; i < entities.size(); i++) {
            SpeedyEntity entity = entities.get(i);
            try {
                queryProcessor.runInTransaction(() -> {
                    try {
                        eventProcessor.triggerEvent(SpeedyEventType.PRE_INSERT, entityMetadata, entity);
                        context.getValidationProcessor().validateCreateRequestEntity(entityMetadata, entity);

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
                    failed.add(new SpeedyPartialFailure(i, she.getStatus(),
                            she.getMessage(), Instant.now().toString(), inputPk, she));
                    log.info("Entity #{} failed in per-entity transaction", i, she);
                } else if (cause instanceof SpeedyHttpRuntimeException sre) {
                    failed.add(new SpeedyPartialFailure(i, sre.getStatus(),
                            sre.getMessage(), Instant.now().toString(), inputPk, sre));
                    log.info("Entity #{} failed in per-entity transaction", i, sre);
                } else {
                    failed.add(new SpeedyPartialFailure(i, 500,
                            e.getMessage(), Instant.now().toString(), inputPk, e));
                    log.info("Entity #{} failed in per-entity transaction", i, e);
                }
            }
        }

        log.info("Transaction committed: entity={}, mode=PER_ENTITY, count={}, succeeded={}, failed={}",
                entityLabel, entities.size(), succeeded.size(), failed.size());

        IResponseSerializerV2 serializer;
        if (failed.isEmpty()) {
            serializer = new JSONSerializerV2(KeyFieldMetadata.class::isInstance,
                    succeeded, 0, new HashSet<>());
        } else if (entities.size() == 1 && !failed.isEmpty()) {
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


    public List<SpeedyEntity> parserContent(RequestContext context, EntityMetadata resourceMetadata, JsonNode jsonElement) throws SpeedyHttpException {
        if (jsonElement == null || !jsonElement.isArray()) {
            throw new BadRequestException("no content to process");
        }
        List<SpeedyEntity> parsedObjects = new LinkedList<>();
        ArrayNode batchOfEntities = (ArrayNode) jsonElement;
        QueryProcessor queryProcessor = context.getQueryProcessor();
        for (JsonNode element : batchOfEntities) {
            if (element.isObject()) {
                ObjectNode objectNode = (ObjectNode) element;
                if (MetadataUtil.isPrimaryKeyComplete(resourceMetadata, objectNode)) {
                    SpeedyEntityKey pk = MetadataUtil.createIdentifierFromJSON(
                            resourceMetadata,
                            objectNode
                    );
                    if (queryProcessor.exists(pk)) {
                        throw new BadRequestException("Entity already present.");
                    }
                }
                SpeedyEntity speedyEntity = MetadataUtil.createEntityFromJSON(
                        resourceMetadata,
                        objectNode
                );
                log.info("parsed entity {}", speedyEntity);
                parsedObjects.add(speedyEntity);
            } else {
                throw new BadRequestException("in-valid content");
            }
        }
        return parsedObjects;
    }
}
