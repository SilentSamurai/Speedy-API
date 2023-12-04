package com.github.silent.samurai.speedy.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyValueImpl;
import com.github.silent.samurai.speedy.utils.CommonUtil;

public class JsonEntityDeserializer {

    private final JsonNode entityJson;
    private final EntityMetadata entityMetadata;

    public JsonEntityDeserializer(JsonNode entityJson, EntityMetadata entityMetadata) throws Exception {
        this.entityJson = entityJson;
        this.entityMetadata = entityMetadata;
    }

    public SpeedyEntity deserialize() throws Exception {
        return createEntity(this.entityMetadata, entityJson);
    }

//    public void deserializeOn(SpeedyEntity speedyEntity) throws Exception {
//        this.speedyEntity = speedyEntity;
//        createEntity(this.entityMetadata, entityJson);
//    }

    private SpeedyEntity createEntity(EntityMetadata entityMetadata, JsonNode entityJson) throws Exception {
        SpeedyEntity speedyEntity = entityMetadata.createNewEntityInstance();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isDeserializable()) continue;
            SpeedyValue value = this.retrieveFieldValue(
                    fieldMetadata, entityJson
            );
            if (value != null) {
                speedyEntity.setBasicValue(fieldMetadata, value);
            }
        }
        return speedyEntity;
    }

    private SpeedyValue retrieveFieldValue(
            FieldMetadata fieldMetadata,
            JsonNode entityObject) throws Exception {
        SpeedyValue value = null;
        String propertyName = fieldMetadata.getOutputPropertyName();
        if (entityObject.has(propertyName)) {
            if (fieldMetadata.isAssociation()) {
                EntityMetadata association = fieldMetadata.getAssociationMetadata();
                if (entityObject.get(propertyName).isObject()) {
                    SpeedyEntity associationEntity = this.createEntity(association, entityObject.get(propertyName));
                    value = SpeedyValueImpl.fromOne(fieldMetadata.getValueType(), associationEntity);
                }
                // array of association
            } else {
                Object po = CommonUtil.jsonToType(entityObject.get(propertyName), fieldMetadata.getFieldType());
                value = SpeedyValueImpl.fromOne(fieldMetadata.getValueType(), po);
            }
        }
        return value;
    }

    private Object createEntityKey(EntityMetadata association, ObjectNode jsonObject) throws Exception {
        JsonIdentityDeserializer deserializer = new JsonIdentityDeserializer(association, jsonObject);
        return deserializer.deserialize();
    }


}
