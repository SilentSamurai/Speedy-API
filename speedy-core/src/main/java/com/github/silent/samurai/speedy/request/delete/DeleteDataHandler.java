package com.github.silent.samurai.speedy.request.delete;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class DeleteDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteDataHandler.class);

    private final DeleteRequestContext context;

    public DeleteDataHandler(DeleteRequestContext context) {
        this.context = context;
    }

    public Optional<List<SpeedyEntity>> process() throws Exception {
        List<SpeedyEntity> deletedObjects = new LinkedList<>();
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        if (!context.getKeysToBeRemoved().isEmpty()) {
            for (SpeedyEntityKey parsedKey : context.getKeysToBeRemoved()) {
                // validate b4 delete
                context.getValidationProcessor().validateDeleteRequestEntity(
                        entityMetadata,
                        parsedKey);
                // pre delete event fired
                eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE,
                        entityMetadata, parsedKey);
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
