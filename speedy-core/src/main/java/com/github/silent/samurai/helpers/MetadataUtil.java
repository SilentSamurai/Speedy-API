package com.github.silent.samurai.helpers;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MetadataUtil {

    public static boolean isPrimaryKeyComplete(EntityMetadata entityMetadata, Set<String> fields) {
        Sets.SetView<String> difference = Sets.intersection(entityMetadata.getKeyFieldNames(), fields);
        return difference.size() == entityMetadata.getKeyFields().size();
    }

    public static boolean hasOnlyPrimaryKeyFields(EntityMetadata entityMetadata, Set<String> fields) {
        if (!fields.isEmpty() && entityMetadata.getKeyFieldNames().size() == fields.size()) {
            Sets.SetView<String> difference = Sets.intersection(entityMetadata.getKeyFieldNames(), fields);
            return difference.size() == entityMetadata.getKeyFieldNames().size();
        }
        return false;
    }

    public static Object createEntityKeyFromMap(EntityMetadata entityMetadata, Map<String, String> fieldMap) throws Exception {
        if (!entityMetadata.hasCompositeKey()) {
            Optional<KeyFieldMetadata> primaryKeyFieldMetadata = entityMetadata.getKeyFields().stream().findAny();
            if (primaryKeyFieldMetadata.isPresent()) {
                KeyFieldMetadata keyFieldMetadata = primaryKeyFieldMetadata.get();
                String propertyName = keyFieldMetadata.getOutputPropertyName();
                if (fieldMap.containsKey(propertyName)) {
                    return CommonUtil.stringToType(fieldMap.get(propertyName), keyFieldMetadata.getFieldType());
                }
            }
            throw new BadRequestException("primary key field not found" + fieldMap);
        }
        Object newKeyInstance = entityMetadata.createNewKeyInstance();
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            String propertyName = keyFieldMetadata.getOutputPropertyName();
            if (fieldMap.containsKey(propertyName)) {
                Object value = CommonUtil.stringToType(fieldMap.get(propertyName), keyFieldMetadata.getFieldType());
                keyFieldMetadata.setEntityFieldWithValue(newKeyInstance, value);
            } else {
                throw new BadRequestException("primary key incomplete" + fieldMap);
            }
        }
        return newKeyInstance;
    }

    public static Object createEntityKeyFromJSON(EntityMetadata entityMetadata, JsonObject jsonObject) throws Exception {
        if (!entityMetadata.hasCompositeKey()) {
            Optional<KeyFieldMetadata> primaryKeyFieldMetadata = entityMetadata.getKeyFields().stream().findAny();
            if (primaryKeyFieldMetadata.isPresent()) {
                KeyFieldMetadata keyFieldMetadata = primaryKeyFieldMetadata.get();
                String propertyName = keyFieldMetadata.getOutputPropertyName();
                if (jsonObject.has(propertyName)) {
                    return CommonUtil.gsonToType(jsonObject.get(propertyName), keyFieldMetadata.getFieldType());
                }
            }
            throw new BadRequestException("primary key field not found" + jsonObject);
        }
        Object newKeyInstance = entityMetadata.createNewKeyInstance();
        for (KeyFieldMetadata fieldMetadata : entityMetadata.getKeyFields()) {
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (jsonObject.has(propertyName)) {
                Object value = CommonUtil.gsonToType(jsonObject.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.setEntityFieldWithValue(newKeyInstance, value);
            } else {
                throw new BadRequestException("primary key incomplete" + jsonObject);
            }
        }
        return newKeyInstance;
    }

    public static Object createEntityObjectFromMap(EntityMetadata entityMetadata, Map<String, String> fieldsMap) throws Exception {
        Object newInstance = entityMetadata.createNewEntityInstance();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (fieldsMap.containsKey(propertyName)) {
                Object value = CommonUtil.stringToType(fieldsMap.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.setEntityFieldWithValue(newInstance, value);
            }
        }
        return newInstance;
    }

    public static Object createEntityObjectFromJSON(EntityMetadata entityMetadata, JsonObject jsonObject) throws Exception {
        Object newInstance = entityMetadata.createNewEntityInstance();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (jsonObject.has(propertyName)) {
                Object value = CommonUtil.gsonToType(jsonObject.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.setEntityFieldWithValue(newInstance, value);
            }
        }
        return newInstance;
    }

    public static void updateEntityFromJson(EntityMetadata entityMetadata, JsonObject jsonObject, Object entity) {
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (jsonObject.has(propertyName)) {
                Object value = CommonUtil.gsonToType(jsonObject.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.setEntityFieldWithValue(entity, value);
            }
        }
    }

}
