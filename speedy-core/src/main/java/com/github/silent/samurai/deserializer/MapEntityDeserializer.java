package com.github.silent.samurai.deserializer;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.utils.CommonUtil;

import javax.persistence.EntityManager;
import java.util.Map;

public class MapEntityDeserializer {

    private final Map<String, String> entityMap;
    private final EntityMetadata entityMetadata;
    private final EntityManager entityManager;
    private Object entityInstance;

    public MapEntityDeserializer(Map<String, String> entityMap, EntityMetadata entityMetadata, EntityManager entityManager) {
        this.entityMap = entityMap;
        this.entityMetadata = entityMetadata;
        this.entityManager = entityManager;
    }

    public Object deserialize() throws Exception {
        entityInstance = entityMetadata.createNewEntityInstance();
        createEntity(this.entityMetadata, entityMap);
        return entityInstance;
    }

    void deserializeOn(Object entityInstance) throws Exception {
        this.entityInstance = entityInstance;
        createEntity(this.entityMetadata, entityMap);
    }

    private void createEntity(EntityMetadata entityMetadata, Map<String, String> fieldsMap) throws Exception {
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isDeserializable()) continue;
            Object value = this.retrieveFieldValue(
                    fieldMetadata, entityMap
            );
            if (value != null) {
                fieldMetadata.setEntityFieldWithValue(this.entityInstance, value);
            }
        }

    }

    private Object retrieveFieldValue(
            FieldMetadata fieldMetadata,
            Map<String, String> fieldsMap) throws Exception {
        Object value = null;
        String propertyName = fieldMetadata.getOutputPropertyName();
        if (fieldsMap.containsKey(propertyName)) {
            if (fieldMetadata.isAssociation()) {
                // TODO: array of association & association
            } else {
                value = CommonUtil.stringToType(fieldsMap.get(propertyName), fieldMetadata.getFieldType());
            }
        }
        return value;
    }

    private Object createEntityKey(EntityMetadata association, Map<String, String> keyMap) throws Exception {
        MapEntityKeyDeserializer deserializer = new MapEntityKeyDeserializer(keyMap, association);
        return deserializer.deserialize();
    }


}
