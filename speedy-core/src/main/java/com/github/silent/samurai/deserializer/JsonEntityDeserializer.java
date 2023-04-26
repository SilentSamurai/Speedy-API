package com.github.silent.samurai.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.utils.CommonUtil;

import javax.persistence.EntityManager;

public class JsonEntityDeserializer {

    private final JsonNode entityJson;
    private final EntityMetadata entityMetadata;
    private final EntityManager entityManager;
    private Object entityInstance;

    public JsonEntityDeserializer(JsonNode entityJson, EntityMetadata entityMetadata, EntityManager entityManager) {
        this.entityJson = entityJson;
        this.entityMetadata = entityMetadata;
        this.entityManager = entityManager;
    }

    public Object deserialize() throws Exception {
        entityInstance = entityMetadata.createNewEntityInstance();
        createEntity(this.entityMetadata, entityJson);
        return entityInstance;
    }

    public void deserializeOn(Object entityInstance) throws Exception {
        this.entityInstance = entityInstance;
        createEntity(this.entityMetadata, entityJson);
    }

    private void createEntity(EntityMetadata entityMetadata, JsonNode entityJson) throws Exception {
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isDeserializable()) continue;
            Object value = this.retrieveFieldValue(
                    fieldMetadata, entityJson
            );
            if (value != null) {
                fieldMetadata.setEntityFieldWithValue(this.entityInstance, value);
            }
        }

    }

    private Object retrieveFieldValue(
            FieldMetadata fieldMetadata,
            JsonNode entityObject) throws Exception {
        Object value = null;
        String propertyName = fieldMetadata.getOutputPropertyName();
        if (entityObject.has(propertyName)) {
            if (fieldMetadata.isAssociation()) {
                EntityMetadata association = fieldMetadata.getAssociationMetadata();
                if (entityObject.get(propertyName).isObject()) {

                    Object primaryKey = this.createEntityKey(association, (ObjectNode) entityObject.get(propertyName));
                    value = entityManager.find(association.getEntityClass(), primaryKey);
                }
                // array of association
            } else {
                value = CommonUtil.jsonToType(entityObject.get(propertyName), fieldMetadata.getFieldType());
            }
        }
        return value;
    }

    private Object createEntityKey(EntityMetadata association, ObjectNode jsonObject) throws Exception {
        JsonIdentityDeserializer deserializer = new JsonIdentityDeserializer(association, jsonObject);
        return deserializer.deserialize();
    }


}
