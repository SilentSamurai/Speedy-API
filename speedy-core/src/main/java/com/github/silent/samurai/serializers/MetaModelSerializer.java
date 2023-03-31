package com.github.silent.samurai.serializers;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collection;

public class MetaModelSerializer {

    public static JsonElement serializeMetaModel(MetaModelProcessor metaModelProcessor) {
        Collection<EntityMetadata> allEntityMetadata = metaModelProcessor.getAllEntityMetadata();
        JsonArray entityArray = new JsonArray();
        allEntityMetadata.stream()
                .map(MetaModelSerializer::serializeEntityMetaModel)
                .forEach(entityArray::add);
        return entityArray;
    }

    public static JsonElement serializeEntityMetaModel(EntityMetadata entityMetadata) {
        JsonObject jsonMetadata = new JsonObject();
        jsonMetadata.addProperty("name", entityMetadata.getName());
        JsonArray fieldArray = new JsonArray();
        entityMetadata.getAllFields().stream()
                .map(MetaModelSerializer::serializeFieldMetadata)
                .forEach(fieldArray::add);
        jsonMetadata.add("fields", fieldArray);

        JsonArray keyArray = new JsonArray();
        entityMetadata.getKeyFields().stream()
                .map(MetaModelSerializer::serializeFieldMetadata)
                .forEach(keyArray::add);
        jsonMetadata.add("keyFields", keyArray);
        jsonMetadata.addProperty("hasCompositeKey", entityMetadata.hasCompositeKey());
        jsonMetadata.addProperty("entityType", entityMetadata.getEntityClass().getName());
        jsonMetadata.addProperty("keyType", entityMetadata.getKeyClass().getName());
        return jsonMetadata;
    }


    public static JsonElement serializeFieldMetadata(FieldMetadata fieldMetadata) {
        JsonObject fieldMetadataJson = new JsonObject();
        fieldMetadataJson.addProperty("className", fieldMetadata.getClassFieldName());
        fieldMetadataJson.addProperty("outputProperty", fieldMetadata.getOutputPropertyName());
        fieldMetadataJson.addProperty("dbColumn", fieldMetadata.getDbColumnName());
        fieldMetadataJson.addProperty("fieldType", fieldMetadata.getFieldType().getName());
        return fieldMetadataJson;
    }


}
