package com.github.silent.samurai.deserializer;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.JsonObject;

import javax.persistence.EntityManager;

public class JsonEntityDeserializer {

    private final JsonObject entityJson;
    private final EntityMetadata entityMetadata;
    private final EntityManager entityManager;
    private Object entityInstance;

    public JsonEntityDeserializer(JsonObject entityJson, EntityMetadata entityMetadata, EntityManager entityManager) {
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

    private void createEntity(EntityMetadata entityMetadata, JsonObject entityJson) throws Exception {
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
            JsonObject entityObject) throws Exception {
        Object value = null;
        String propertyName = fieldMetadata.getOutputPropertyName();
        if (entityObject.has(propertyName)) {
            if (fieldMetadata.isAssociation()) {
                EntityMetadata association = fieldMetadata.getAssociationMetadata();
                if (entityObject.get(propertyName).isJsonObject()) {

                    Object primaryKey = this.createEntityKey(association, (JsonObject) entityObject.get(propertyName));
                    value = entityManager.find(association.getEntityClass(), primaryKey);
                }
                // array of association
            } else {
                value = CommonUtil.gsonToType(entityObject.get(propertyName), fieldMetadata.getFieldType());
            }
        }
        return value;
    }

    private Object createEntityKey(EntityMetadata association, JsonObject jsonObject) throws Exception {
        JsonEntityKeyDeserializer deserializer = new JsonEntityKeyDeserializer(association, jsonObject);
        return deserializer.deserialize();
    }


}
