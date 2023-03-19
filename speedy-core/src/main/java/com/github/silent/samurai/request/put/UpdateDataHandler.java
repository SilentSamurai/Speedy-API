package com.github.silent.samurai.request.put;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityTransaction;
import javax.validation.ConstraintViolation;
import java.io.IOException;
import java.util.Set;

public class UpdateDataHandler {

    Logger logger = LogManager.getLogger(UpdateDataHandler.class);

    private final PutRequestContext context;

    public UpdateDataHandler(PutRequestContext context) {
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
        context.getEntityManager().persist(entityInstance);
        context.getEntityManager().flush();
        logger.info("{} saved {}", entityMetadata.getName(), entityInstance);
    }

    public void process() {
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
