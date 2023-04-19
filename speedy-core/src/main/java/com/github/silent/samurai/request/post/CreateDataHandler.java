package com.github.silent.samurai.request.post;

import com.github.silent.samurai.interfaces.EntityMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityTransaction;

public class CreateDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateDataHandler.class);

    private final PostRequestContext context;

    public CreateDataHandler(PostRequestContext context) {
        this.context = context;
    }

    private void saveEntity(Object entityInstance, EntityMetadata entityMetadata) {
        context.getEntityManager().merge(entityInstance);
        context.getEntityManager().flush();
        LOGGER.info("{} saved {}", entityMetadata.getName(), entityInstance);
    }

    public void processBatch() throws Exception {
        EntityTransaction transaction = context.getEntityManager().getTransaction();
        try {
            if (!context.getParsedObjects().isEmpty()) {
                transaction.begin();
                EntityMetadata entityMetadata = context.getEntityMetadata();
                for (Object parsedObject : context.getParsedObjects()) {
                    context.getValidationProcessor().validateCreateRequestEntity(entityMetadata, parsedObject);
                    saveEntity(parsedObject, entityMetadata);
                }
                transaction.commit();
            }
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
    }

}
