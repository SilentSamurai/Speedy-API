package com.github.silent.samurai.speedy.request.delete;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityTransaction;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class DeleteDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteDataHandler.class);

    private final DeleteRequestContext context;

    public DeleteDataHandler(DeleteRequestContext context) {
        this.context = context;
    }

    private Object deleteEntity(Object parsedObject, EntityMetadata entityMetadata) {
        EntityTransaction transaction = context.getEntityManager().getTransaction();
        try {
            transaction.begin();
            context.getEntityManager().remove(parsedObject);
            LOGGER.info("{} deleted {}", entityMetadata.getName(), parsedObject);
            transaction.commit();
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
        return parsedObject;
    }

    public Optional<List<Object>> process() throws Exception {

        List<Object> deletedObjects = new LinkedList<>();
        EntityMetadata entityMetadata = context.getParser().getPrimaryResource().getResourceMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        if (!context.getObjectsToBeRemoved().isEmpty()) {
            for (Object parsedObject : context.getObjectsToBeRemoved()) {
                // validate b4 delete
                context.getValidationProcessor().validateDeleteRequestEntity(
                        entityMetadata,
                        parsedObject);
                // pre delete event fired
                eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE,
                        entityMetadata, parsedObject);
                // handle delete request
                if (context.getVEntityProcessor().isVirtualEntity(entityMetadata)) {
                    SpeedyVirtualEntityHandler<Object> handler = context.getVEntityProcessor().getHandler(entityMetadata);
                    handler.delete(parsedObject);
                } else {
                    deleteEntity(parsedObject, entityMetadata);
                }
                deletedObjects.add(parsedObject);
                // fire post delete event
                eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE,
                        entityMetadata, parsedObject);
            }
        }
        return Optional.of(deletedObjects);
    }
}
