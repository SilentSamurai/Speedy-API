package com.github.silent.samurai.request.post;

import com.github.silent.samurai.interfaces.EntityMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityTransaction;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class CreateDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateDataHandler.class);

    private final PostRequestContext context;

    public CreateDataHandler(PostRequestContext context) {
        this.context = context;
    }

    private Object saveEntity(Object entityInstance, EntityMetadata entityMetadata) {
        entityInstance = context.getEntityManager().merge(entityInstance);
        context.getEntityManager().flush();
        LOGGER.info("{} saved {}", entityMetadata.getName(), entityInstance);
        return entityInstance;
    }

    public Optional<List<Object>> processBatch() throws Exception {
        EntityTransaction transaction = context.getEntityManager().getTransaction();
        List<Object> savedObjects = new LinkedList<>();
        try {
            if (!context.getParsedObjects().isEmpty()) {
                transaction.begin();
                EntityMetadata entityMetadata = context.getEntityMetadata();
                for (Object parsedObject : context.getParsedObjects()) {
                    context.getValidationProcessor().validateCreateRequestEntity(entityMetadata, parsedObject);
                    Object savedEntity = saveEntity(parsedObject, entityMetadata);
                    savedObjects.add(savedEntity);
                }
                transaction.commit();
            }
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
        return Optional.of(savedObjects);
    }

}
