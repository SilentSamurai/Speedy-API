package com.github.silent.samurai.speedy.request.post;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityTransaction;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class PostDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostDataHandler.class);

    private final PostRequestContext context;

    public PostDataHandler(PostRequestContext context) {
        this.context = context;
    }

    private void processVirtual(List<Object> savedObjects) throws Exception {
        EventProcessor eventProcessor = context.getEventProcessor();
        EntityMetadata entityMetadata = context.getEntityMetadata();
        for (Object parsedObject : context.getParsedObjects()) {
            // validation
            context.getValidationProcessor().validateCreateRequestEntity(entityMetadata, parsedObject);
            // event trigger
            eventProcessor.triggerEvent(SpeedyEventType.PRE_INSERT,
                    entityMetadata, parsedObject);
            // save handler trigger
            SpeedyVirtualEntityHandler<Object> handler = context.getVEntityProcessor().getHandler(entityMetadata);
            Object savedEntity = handler.create(parsedObject);
            if (!MetadataUtil.isKeyCompleteInEntity(entityMetadata, savedEntity)) {
                throw new BadRequestException("Incomplete Key after save");
            }
            savedObjects.add(savedEntity);
            eventProcessor.triggerEvent(SpeedyEventType.POST_INSERT,
                    entityMetadata, parsedObject);
        }
    }

    private void processPhysical(List<Object> savedObjects) throws Exception {
        EventProcessor eventProcessor = context.getEventProcessor();
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EntityTransaction transaction = context.getEntityManager().getTransaction();
        transaction.begin();
        for (Object parsedObject : context.getParsedObjects()) {
            try {
                // validate entity
                context.getValidationProcessor().validateCreateRequestEntity(entityMetadata, parsedObject);
                // trigger pre insert event
                eventProcessor.triggerEvent(SpeedyEventType.PRE_INSERT,
                        entityMetadata, parsedObject);
                // save the entity
                Object savedEntity = context.getEntityManager().merge(parsedObject);
                context.getEntityManager().flush();
                LOGGER.info("{} saved {}", entityMetadata.getName(), parsedObject);
                // check if primary key is complete
                if (!MetadataUtil.isKeyCompleteInEntity(entityMetadata, savedEntity)) {
                    throw new BadRequestException("Incomplete Key after save");
                }
                // trigger post insert event
                eventProcessor.triggerEvent(SpeedyEventType.POST_INSERT,
                        entityMetadata, parsedObject);
                // add to saved objects
                savedObjects.add(savedEntity);

            } catch (Throwable throwable) {
                // rollback all changes if any exception occurs
                transaction.rollback();
                throw throwable;
            }
        }
        // commit only if all entities are saved
        transaction.commit();
    }

    public Optional<List<Object>> processBatch() throws Exception {
        List<Object> savedObjects = new LinkedList<>();
        if (!context.getParsedObjects().isEmpty()) {
            EntityMetadata entityMetadata = context.getEntityMetadata();
            if (context.getVEntityProcessor().isVirtualEntity(entityMetadata)) {
                processVirtual(savedObjects);
            } else {
                processPhysical(savedObjects);
            }
        }
        return Optional.of(savedObjects);
    }

}
