package com.github.silent.samurai.request.put;

import com.github.silent.samurai.interfaces.EntityMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityTransaction;

public class UpdateDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDataHandler.class);

    private final PutRequestContext context;

    public UpdateDataHandler(PutRequestContext context) {
        this.context = context;
    }

    private void saveEntity(Object entityInstance, EntityMetadata entityMetadata) throws Exception {
        context.getValidationProcessor().validateUpdateRequestEntity(entityMetadata, entityInstance);
        context.getEntityManager().persist(entityInstance);
        context.getEntityManager().flush();
        LOGGER.info("{} saved {}", entityMetadata.getName(), entityInstance);
    }

    public void process() throws Exception {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EntityTransaction transaction = context.getEntityManager().getTransaction();
        try {
            Object entityInstance = context.getEntityInstance();
            if (entityInstance != null) {
                transaction.begin();
                saveEntity(entityInstance, entityMetadata);
                transaction.commit();
            }
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
    }
}
