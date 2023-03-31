package com.github.silent.samurai.request.put;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityTransaction;
import javax.validation.ConstraintViolation;
import java.util.Set;

public class UpdateDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDataHandler.class);

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
        LOGGER.info("{} saved {}", entityMetadata.getName(), entityInstance);
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
