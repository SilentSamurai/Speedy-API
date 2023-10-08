package com.github.silent.samurai.speedy.request.delete;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
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
            LOGGER.info("{} saved {}", entityMetadata.getName(), parsedObject);
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

                context.getValidationProcessor().validateDeleteRequestEntity(
                        entityMetadata,
                        parsedObject);

                eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE,
                        entityMetadata, parsedObject);

                if (eventProcessor.isEventPresent(SpeedyEventType.IN_PLACE_OF_DELETE, entityMetadata)) {
                    eventProcessor.triggerEvent(SpeedyEventType.IN_PLACE_OF_DELETE,
                            entityMetadata, parsedObject);
                } else {
                    deleteEntity(parsedObject, entityMetadata);
                }
                deletedObjects.add(parsedObject);

                eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE,
                        entityMetadata, parsedObject);

            }
        }
        return Optional.of(deletedObjects);
    }
}
