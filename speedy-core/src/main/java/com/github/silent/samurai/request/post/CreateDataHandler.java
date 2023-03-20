package com.github.silent.samurai.request.post;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityTransaction;
import javax.validation.ConstraintViolation;
import java.util.Set;

public class CreateDataHandler {

    Logger logger = LogManager.getLogger(CreateDataHandler.class);

    private final PostRequestContext context;

    public CreateDataHandler(PostRequestContext context) {
        this.context = context;
    }

    private void saveEntity(Object entityInstance, EntityMetadata entityMetadata) {
        Set<ConstraintViolation<Object>> constraintViolations = context.getValidator().validate(entityInstance);
        if (!constraintViolations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<Object> violation : constraintViolations) {
                sb.append(violation.getMessage()).append(" | ");
            }
            throw new BadRequestException(sb.toString());
        }
        context.getEntityManager().merge(entityInstance);
        context.getEntityManager().flush();
        logger.info("{} saved {}", entityMetadata.getName(), entityInstance);
    }

    public void processBatch() {
        EntityTransaction transaction = context.getEntityManager().getTransaction();
        try {
            if (!context.getParsedObjects().isEmpty()) {
                transaction.begin();
                EntityMetadata entityMetadata = context.getEntityMetadata();
                for (Object parsedObject : context.getParsedObjects()) {
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
