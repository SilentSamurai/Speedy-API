package com.github.silent.samurai.helpers;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.utils.CommonUtil;
import com.github.silent.samurai.utils.MapUtils;
import com.github.silent.samurai.utils.TypeUtils;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

public class MetadataUtil {

    public static boolean isPrimaryKeyComplete(EntityMetadata entityMetadata, Set<String> fields) {
        Sets.SetView<String> difference = Sets.intersection(entityMetadata.getKeyFields(), fields);
        return difference.size() == entityMetadata.getKeyFields().size();
    }

    public static boolean hasOnlyPrimaryKeyFields(EntityMetadata entityMetadata, Set<String> fields) {
        if (entityMetadata.getKeyFields().size() == fields.size()) {
            Sets.SetView<String> difference = Sets.intersection(entityMetadata.getKeyFields(), fields);
            return difference.size() == entityMetadata.getKeyFields().size();
        }
        return false;
    }

    public static Object getPrimaryKey(EntityMetadata entityMetadata, Map<String, String> fieldMap) throws Exception {
        if (TypeUtils.isPrimaryType(entityMetadata.getKeyClass())) {
            return MapUtils.findAnyValueInMap(fieldMap, entityMetadata.getKeyClass());
        }
        Object newKeyInstance = entityMetadata.createNewKeyInstance();
        for (String keyField : entityMetadata.getKeyFields()) {
            FieldMetadata fieldMetadata = entityMetadata.field(keyField);
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (fieldMap.containsKey(propertyName)) {
                Object value = CommonUtil.stringToType(fieldMap.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.updateClassFieldWithValue(newKeyInstance, value);
            }
        }
        return newKeyInstance;
    }

    public static Object getPrimaryKey(EntityMetadata entityMetadata, JsonObject jsonObject) throws Exception {
        if (TypeUtils.isPrimaryType(entityMetadata.getKeyClass())) {
            return MapUtils.findAnyValueInJsonObject(jsonObject, entityMetadata.getKeyClass());
        }
        Object newKeyInstance = entityMetadata.createNewKeyInstance();
        for (String keyField : entityMetadata.getKeyFields()) {
            FieldMetadata fieldMetadata = entityMetadata.field(keyField);
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (jsonObject.has(propertyName)) {
                Object value = CommonUtil.gsonToType(jsonObject.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.updateClassFieldWithValue(newKeyInstance, value);
            }
        }
        return newKeyInstance;
    }

    public static Object getObject(EntityMetadata entityMetadata, Map<String, String> fieldsMap) throws Exception {
        Object newInstance = entityMetadata.createNewEntityInstance();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (fieldsMap.containsKey(propertyName)) {
                Object value = CommonUtil.stringToType(fieldsMap.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.updateClassFieldWithValue(newInstance, value);
            }
        }
        return newInstance;
    }

    public static Object getObject(EntityMetadata entityMetadata, JsonObject jsonObject) throws Exception {
        Object newInstance = entityMetadata.createNewEntityInstance();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (jsonObject.has(propertyName)) {
                Object value = CommonUtil.gsonToType(jsonObject.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.updateClassFieldWithValue(newInstance, value);
            }
        }
        return newInstance;
    }

    public static void updateEntityFromJson(EntityMetadata entityMetadata, JsonObject jsonObject, Object entity) {
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (jsonObject.has(propertyName)) {
                Object value = CommonUtil.gsonToType(jsonObject.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.updateClassFieldWithValue(entity, value);
            }
        }
    }

}
