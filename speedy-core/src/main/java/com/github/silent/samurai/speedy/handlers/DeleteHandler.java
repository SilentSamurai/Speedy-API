package com.github.silent.samurai.speedy.handlers;

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
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class DeleteHandler implements Handler {

    final Handler next;

    public DeleteHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();

        Optional<List<SpeedyEntity>> speedyEntities;
        List<SpeedyEntityKey> keysToBeRemoved = parse(context);
        try {
            speedyEntities = runBatchQuery(context, keysToBeRemoved);
        } catch (SpeedyHttpException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerError("Delete failed", e);
        }
        if (speedyEntities.isPresent()) {
            context.setResponseSerializer(new JSONSerializerV2(
                    KeyFieldMetadata.class::isInstance,
                    speedyEntities.get(),
                    0,
                    new HashSet<>()
            ));
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
                if (!queryProcessor.exists(pk)) {
                    throw new BadRequestException("entity not found");
                }
                keysToBeRemoved.add(pk);
                log.info("parsed primary key {}", pk);
            } else {
                throw new BadRequestException("in-valid request body");
            }
        }
        return keysToBeRemoved;
    }

    public Optional<List<SpeedyEntity>> runBatchQuery(RequestContext context, List<SpeedyEntityKey> keysToBeRemoved) throws Exception {
        List<SpeedyEntity> deletedObjects = List.of();
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
            }

            // handle delete request
            deletedObjects = queryProcessor.delete(keysToBeRemoved);

            for (SpeedyEntityKey parsedKey : keysToBeRemoved) {
                // fire post delete event
                eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE,
                        entityMetadata, parsedKey);
            }

        }
        return Optional.ofNullable(deletedObjects);
    }
}
