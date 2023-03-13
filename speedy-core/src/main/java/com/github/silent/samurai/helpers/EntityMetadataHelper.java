package com.github.silent.samurai.helpers;

import com.github.silent.samurai.enums.IgnoreType;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.metamodel.RequestInfo;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class EntityMetadataHelper {

    public static EntityMetadataHelper instance = new EntityMetadataHelper();

    public boolean isPrimaryKeyComplete(EntityMetadata entityMetadata, Set<String> fields) {
        // returns fields present in idFields and not in fields
        Sets.SetView<String> difference = Sets.difference(entityMetadata.getKeyFields(), fields);
        return difference.isEmpty();
    }

    public boolean isOnlyPrimaryKeyFields(EntityMetadata entityMetadata, Set<String> fields) {
        Sets.SetView<String> difference = Sets.difference(fields, entityMetadata.getKeyFields());
        return difference.isEmpty();
    }

    public Object getPrimaryKey(EntityMetadata entityMetadata, RequestInfo requestInfo) {
        if (Objects.equals(entityMetadata.getKeyClass(), String.class)) {
            return requestInfo.filters.get("id");
        }
        return CommonUtil.mapModel(requestInfo.filters, entityMetadata.getKeyClass());
    }

    public Object getPrimaryKey(EntityMetadata entityMetadata, JsonObject fieldsMap) {
        if (Objects.equals(entityMetadata.getKeyClass(), String.class)) {
            return fieldsMap.get("id").getAsString();
        }
        return CommonUtil.getGson().fromJson(fieldsMap, entityMetadata.getKeyClass());
    }

    public Object getObject(EntityMetadata entityMetadata, Map<String, ?> fieldsMap) {
        return CommonUtil.mapModel(fieldsMap, entityMetadata.getEntityClass());
    }

    public Object getObject(EntityMetadata entityMetadata, JsonObject fieldsMap) {
        for (FieldMetadata memberMetadata : entityMetadata.getAllFields()) {
            if (memberMetadata.getIgnoreType() != null && memberMetadata.getIgnoreType() == IgnoreType.PERSIST) {
                fieldsMap.remove(memberMetadata.getFieldName());
            }
        }
        return CommonUtil.getGson().fromJson(fieldsMap, entityMetadata.getEntityClass());
    }

    public void updateObject(EntityMetadata entityMetadata, JsonObject fieldsMap, Object entity) {
        Object updatedRequest = this.getObject(entityMetadata, fieldsMap);
        CommonUtil.mapModel(updatedRequest, entity);
    }

}
