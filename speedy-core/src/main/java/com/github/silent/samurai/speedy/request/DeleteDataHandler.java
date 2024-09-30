package com.github.silent.samurai.speedy.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class DeleteDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteDataHandler.class);

    private final IRequestContextImpl context;
    private final List<SpeedyEntityKey> keysToBeRemoved = new LinkedList<>();

    public DeleteDataHandler(IRequestContextImpl context) {
        this.context = context;
    }

    public Optional<List<SpeedyEntity>> process() throws Exception {
        parse();
        return runBatchQuery();
    }

    public void parse() throws Exception {
        EntityMetadata resourceMetadata = context.getEntityMetadata();
        QueryProcessor queryProcessor = context.getQueryProcessor();

        ObjectMapper json = CommonUtil.json();
        JsonNode jsonElement = json.readTree(context.getRequest().getReader());
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
                if (!queryProcessor.exists(pk)) {
                    throw new BadRequestException("entity not found");
                }
                keysToBeRemoved.add(pk);
                LOGGER.info("parsed primary key {}", pk);
            } else {
                throw new BadRequestException("in-valid request body");
            }
        }
    }

    public Optional<List<SpeedyEntity>> runBatchQuery() throws Exception {
        List<SpeedyEntity> deletedObjects = new LinkedList<>();
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        if (!keysToBeRemoved.isEmpty()) {
            for (SpeedyEntityKey parsedKey : keysToBeRemoved) {
                // validate b4 delete
                context.getValidationProcessor().validateDeleteRequestEntity(
                        entityMetadata,
                        parsedKey);
                // pre delete event fired
                eventProcessor.triggerEvent(
                        SpeedyEventType.PRE_DELETE,
                        entityMetadata,
                        parsedKey);
                // handle delete request
                if (context.getVEntityProcessor().isVirtualEntity(entityMetadata)) {
                    SpeedyVirtualEntityHandler handler = context.getVEntityProcessor().getHandler(entityMetadata);
                    SpeedyEntity deletedEntity = handler.delete(parsedKey);
                    deletedObjects.add(deletedEntity);
                } else {
                    SpeedyEntity deletedEntity = queryProcessor.delete(parsedKey);
                    deletedObjects.add(deletedEntity);
                }

                // fire post delete event
                eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE,
                        entityMetadata, parsedKey);
            }
        }
        return Optional.of(deletedObjects);
    }
}
