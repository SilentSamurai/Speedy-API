package com.github.silent.samurai.speedy.request.post;

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

public class PostDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostDataHandler.class);

    private final PostRequestContext context;

    public PostDataHandler(PostRequestContext context) {
        this.context = context;
    }

    private Object saveEntity(Object entityInstance, EntityMetadata entityMetadata) {
        EntityTransaction transaction = context.getEntityManager().getTransaction();
        try {
            transaction.begin();
            entityInstance = context.getEntityManager().merge(entityInstance);
            context.getEntityManager().flush();
            LOGGER.info("{} saved {}", entityMetadata.getName(), entityInstance);
            transaction.commit();
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
        return entityInstance;
    }

    public Optional<List<Object>> processBatch() throws Exception {
        List<Object> savedObjects = new LinkedList<>();
        EventProcessor eventProcessor = context.getEventProcessor();
        if (!context.getParsedObjects().isEmpty()) {
            EntityMetadata entityMetadata = context.getParser().getPrimaryResource().getResourceMetadata();
            for (Object parsedObject : context.getParsedObjects()) {

                context.getValidationProcessor().validateCreateRequestEntity(entityMetadata, parsedObject);

                eventProcessor.triggerEvent(SpeedyEventType.PRE_INSERT,
                        entityMetadata, parsedObject);

                if (context.getVEntityProcessor().isVirtualEntity(entityMetadata)) {
                    SpeedyVirtualEntityHandler<Object> handler = context.getVEntityProcessor().getHandler(entityMetadata);
                    Object savedEntity = handler.create(parsedObject);
                    savedObjects.add(savedEntity);
                } else {
                    Object savedEntity = saveEntity(parsedObject, entityMetadata);
                    savedObjects.add(savedEntity);
                }

                eventProcessor.triggerEvent(SpeedyEventType.POST_INSERT,
                        entityMetadata, parsedObject);
            }
        }
        return Optional.of(savedObjects);
    }

}
