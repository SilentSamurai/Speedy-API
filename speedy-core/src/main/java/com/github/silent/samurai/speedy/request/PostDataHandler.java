package com.github.silent.samurai.speedy.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class PostDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostDataHandler.class);

    private final IRequestContextImpl context;

    public PostDataHandler(IRequestContextImpl context) {
        this.context = context;
    }

    private List<SpeedyEntity> processPhysical(List<SpeedyEntity> parsedEntities) throws SpeedyHttpException {
        EventProcessor eventProcessor = context.getEventProcessor();
        EntityMetadata entityMetadata = context.getEntityMetadata();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        List<SpeedyEntity> savedObjects;
        try {
            for (SpeedyEntity parsedObject : parsedEntities) {
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

            savedObjects = queryProcessor.create(parsedEntities);

            for (SpeedyEntity savedEntity : savedObjects) {
                if (savedEntity == null || savedEntity.isEmpty()) {
                    LOGGER.info("{} save failed", entityMetadata.getName());
                } else {
                    LOGGER.info("{} saved {}", entityMetadata.getName(), savedEntity);
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
            throw new InternalServerError(e);
        }
        return savedObjects;
    }

    public Optional<List<SpeedyEntity>> process(EntityMetadata resourceMetadata, JsonNode jsonElement) throws SpeedyHttpException {
        if (!resourceMetadata.isCreateAllowed()) {
            throw new BadRequestException(String.format("create not allowed for %s", resourceMetadata.getName()));
        }

        List<SpeedyEntity> speedyEntities = parserContent(resourceMetadata, jsonElement);
        if (speedyEntities.isEmpty()) {
            return Optional.empty();
        }
        List<SpeedyEntity> savedObjects  = processPhysical(speedyEntities);
        return Optional.of(savedObjects);
    }

    public List<SpeedyEntity> parserContent(EntityMetadata resourceMetadata, JsonNode jsonElement) throws SpeedyHttpException {
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
                LOGGER.info("parsed entity {}", speedyEntity);
                parsedObjects.add(speedyEntity);
            } else {
                throw new BadRequestException("in-valid content");
            }
        }
        return parsedObjects;
    }

}
