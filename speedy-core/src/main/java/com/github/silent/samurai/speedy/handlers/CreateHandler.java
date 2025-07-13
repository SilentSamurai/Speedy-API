package com.github.silent.samurai.speedy.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;

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
        List<SpeedyEntity> savedObjects = new ArrayList<>();
        if (!speedyEntities.isEmpty()) {
            savedObjects = processPhysical(context, speedyEntities);
        }

        IResponseSerializerV2 jsonSerializer = new JSONSerializerV2(
                KeyFieldMetadata.class::isInstance,
                savedObjects,
                0,
                new HashSet<>()
        );
        context.setResponseSerializer(jsonSerializer);


        next.process(context);
    }

    private List<SpeedyEntity> processPhysical(RequestContext context, List<SpeedyEntity> jsonBody) throws SpeedyHttpException {
        EventProcessor eventProcessor = context.getEventProcessor();
        EntityMetadata entityMetadata = context.getEntityMetadata();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        List<SpeedyEntity> savedObjects;
        try {
            for (SpeedyEntity parsedObject : jsonBody) {
                // trigger should go first, then validate the entire thing
                // trigger pre insert event
                eventProcessor.triggerEvent(
                        SpeedyEventType.PRE_INSERT,
                        entityMetadata,
                        parsedObject
                );
                // validate entity
                context.getValidationProcessor().validateCreateRequestEntity(entityMetadata, parsedObject);
            }

            savedObjects = queryProcessor.create(jsonBody);

            for (SpeedyEntity savedEntity : savedObjects) {
                if (savedEntity == null || savedEntity.isEmpty()) {
                    log.info("{} save failed", entityMetadata.getName());
                } else {
                    log.info("{} saved {}", entityMetadata.getName(), savedEntity);
                }

                // TODO: remove this may b not good to throw exception right after insert
                // check if primary key is complete
                if (!MetadataUtil.isKeyCompleteInEntity(entityMetadata, savedEntity)) {
                    throw new BadRequestException("Incomplete Key after save");
                }
                // trigger post insert event
                eventProcessor.triggerEvent(
                        SpeedyEventType.POST_INSERT,
                        entityMetadata,
                        savedEntity
                );
            }

        } catch (SpeedyHttpException throwable) {
            throw throwable;
        } catch (Exception e) {
            throw new InternalServerError("Internal Server Error", e);
        }
        return savedObjects;
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
