package com.github.silent.samurai.request.post;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.exceptions.ResourceNotFoundException;
import com.github.silent.samurai.helpers.EntityMetadataHelper;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;

public class POSTRequestProcessor {

    private final MetaModelProcessor metaModelProcessor;
    private final EntityManager entityManager;
    private final Validator validator;
    Logger logger = LogManager.getLogger(POSTRequestProcessor.class);

    public POSTRequestProcessor(MetaModelProcessor metaModelProcessor, EntityManager entityManager) {
        this.metaModelProcessor = metaModelProcessor;
        this.entityManager = entityManager;
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    void processSave(Object entityInstance, EntityMetadata entityMetadata) {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(entityInstance);
        if (!constraintViolations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<Object> violation : constraintViolations) {
                sb.append(violation.getMessage()).append(" | ");
            }
            throw new BadRequestException(sb.toString());
        }

        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(entityInstance);
            entityManager.flush();
            transaction.commit();
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }

        logger.info("{} saved {}", entityMetadata.getName(), entityInstance);
    }

    void processUpdate(JsonElement jsonElement) {
        JsonObject asJsonObject = jsonElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entities : asJsonObject.entrySet()) {
            EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(entities.getKey());
            if (entityMetadata == null) {
                throw new ResourceNotFoundException("Entity Not Found " + entities.getKey());
            }

            if (!EntityMetadataHelper.instance.isPrimaryKeyComplete(entityMetadata, entities.getValue().getAsJsonObject().keySet())) {
                throw new RuntimeException("Primary Key Incomplete ");
            }

            Object pk = EntityMetadataHelper.instance
                    .getPrimaryKey(entityMetadata, entities.getValue().getAsJsonObject());
            Object entityInstance = null;
            EntityMetadataHelper.instance
                    .updateObject(entityMetadata, entities.getValue().getAsJsonObject(), entityInstance);

            logger.info(" test {}", entityInstance);

            processSave(entityInstance, entityMetadata);
        }

    }

    void processCreate(JsonElement jsonElement) {
        JsonObject asJsonObject = jsonElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entities : asJsonObject.entrySet()) {
            EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(entities.getKey());
            if (entityMetadata == null) {
                throw new ResourceNotFoundException("Entity Not Found " + entities.getKey());
            }
            Object entityInstance = EntityMetadataHelper.instance
                    .getObject(entityMetadata, entities.getValue().getAsJsonObject());
            logger.info("update entity {}", entityInstance);

            processSave(entityInstance, entityMetadata);
        }
    }

    void processDelete(JsonElement jsonElement) {
        JsonObject asJsonObject = jsonElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entities : asJsonObject.entrySet()) {
            EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(entities.getKey());
            if (entityMetadata == null) {
                throw new ResourceNotFoundException("Entity Not Found " + entities.getKey());
            }

            if (!EntityMetadataHelper.instance.isPrimaryKeyComplete(entityMetadata, entities.getValue().getAsJsonObject().keySet())) {
                throw new BadRequestException("Primary Key Incomplete ");
            }

            Object pk = EntityMetadataHelper.instance
                    .getPrimaryKey(entityMetadata, entities.getValue().getAsJsonObject());
            Object entityInstance = entityManager.find(entityMetadata.getEntityClass(), pk);
            entityManager.remove(entityInstance);
            logger.info("removed entity {}", entityInstance);

        }
    }

    public void process(JsonElement jsonElement) {
        JsonObject asJsonObject = jsonElement.getAsJsonObject();
        if (asJsonObject.has("CREATE")) {
            processCreate(asJsonObject.get("CREATE"));
        }
        if (asJsonObject.has("UPDATE")) {
            processUpdate(asJsonObject.get("UPDATE"));
        }
        if (asJsonObject.has("DELETE")) {
            processDelete(asJsonObject.get("DELETE"));
        }
    }
}
