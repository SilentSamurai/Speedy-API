package com.github.silent.samurai.speedy.request.put;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityTransaction;
import java.util.Optional;

public class UpdateDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDataHandler.class);

    private final PutRequestContext context;

    public UpdateDataHandler(PutRequestContext context) {
        this.context = context;
    }

    private Object saveEntity(Object entityInstance, EntityMetadata entityMetadata) throws Exception {
        EntityTransaction transaction = context.getEntityManager().getTransaction();
        try {
            transaction.begin();
            context.getEntityManager().persist(entityInstance);
            context.getEntityManager().flush();
            LOGGER.info("{} saved {}", entityMetadata.getName(), entityInstance);
            transaction.commit();
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
        return entityInstance;
    }

    public Optional<Object> process() throws Exception {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        Object savedEntity = null;
        Object entityInstance = context.getEntityInstance();
        if (entityInstance != null) {
            context.getValidationProcessor().validateUpdateRequestEntity(entityMetadata, entityInstance);

            eventProcessor.triggerEvent(SpeedyEventType.PRE_UPDATE,
                    entityMetadata, entityInstance);
            if (context.getVEntityProcessor().isVirtualEntity(entityMetadata)) {
                SpeedyVirtualEntityHandler<Object> handler = context.getVEntityProcessor().getHandler(entityMetadata);
                savedEntity = handler.update(entityInstance);
            } else {
                savedEntity = saveEntity(entityInstance, entityMetadata);
            }
            eventProcessor.triggerEvent(SpeedyEventType.POST_UPDATE,
                    entityMetadata, entityInstance);

        }

        return Optional.ofNullable(savedEntity);
    }
}
