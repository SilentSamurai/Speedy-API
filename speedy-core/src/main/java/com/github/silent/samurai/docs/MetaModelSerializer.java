package com.github.silent.samurai.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.util.Collection;

public class MetaModelSerializer {

    public static JsonNode serializeMetaModel(MetaModelProcessor metaModelProcessor) {
        Collection<EntityMetadata> allEntityMetadata = metaModelProcessor.getAllEntityMetadata();
        ArrayNode entityArray = CommonUtil.json().createArrayNode();
        allEntityMetadata.stream()
                .map(MetaModelSerializer::serializeEntityMetaModel)
                .forEach(entityArray::add);
        return entityArray;
    }

    public static JsonNode serializeEntityMetaModel(EntityMetadata entityMetadata) {
        ObjectMapper json = CommonUtil.json();
        ObjectNode jsonMetadata = json.createObjectNode();
        jsonMetadata.put("name", entityMetadata.getName());
        ArrayNode fieldArray = json.createArrayNode();
        entityMetadata.getAllFields().stream()
                .map(MetaModelSerializer::serializeFieldMetadata)
                .forEach(fieldArray::add);
        jsonMetadata.set("fields", fieldArray);

        ArrayNode keyArray = json.createArrayNode();
        entityMetadata.getKeyFields().stream()
                .map(MetaModelSerializer::serializeFieldMetadata)
                .forEach(keyArray::add);
        jsonMetadata.set("keyFields", keyArray);
        jsonMetadata.put("hasCompositeKey", entityMetadata.hasCompositeKey());
        jsonMetadata.put("entityType", entityMetadata.getEntityClass().getName());
        jsonMetadata.put("keyType", entityMetadata.getKeyClass().getName());
        return jsonMetadata;
    }


    public static JsonNode serializeFieldMetadata(FieldMetadata fieldMetadata) {
        ObjectNode fieldMetadataJson = CommonUtil.json().createObjectNode();
        fieldMetadataJson.put("className", fieldMetadata.getClassFieldName());
        fieldMetadataJson.put("outputProperty", fieldMetadata.getOutputPropertyName());
        fieldMetadataJson.put("dbColumn", fieldMetadata.getDbColumnName());
        fieldMetadataJson.put("fieldType", fieldMetadata.getFieldType().getName());
        fieldMetadataJson.put("isNullable", fieldMetadata.isNullable());
        fieldMetadataJson.put("isAssociation", fieldMetadata.isAssociation());
        fieldMetadataJson.put("isCollection", fieldMetadata.isCollection());
        fieldMetadataJson.put("isSerializable", fieldMetadata.isSerializable());
        fieldMetadataJson.put("isDeserializable", fieldMetadata.isDeserializable());
        fieldMetadataJson.put("isUnique", fieldMetadata.isUnique());

        return fieldMetadataJson;
    }


}
