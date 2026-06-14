package com.github.silent.samurai.speedy.json.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.models.SpeedyMetadataResponse;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Collection;

/// Serializes the metamodel to JSON and writes the `$metadata` document.
/// The metadata writer: {@link JSONResponseSerializer} delegates {@code writeMetadata} straight to it.
public class JsonMetaModelSerializer {

    public void write(SpeedyMetadataResponse metadataResponse, HttpServletResponse httpResponse) throws SpeedyHttpException {
        JsonNode jsonElement = serializeMetaModel(metadataResponse.getMetaModel());
        JsonHttpWriter.writeJson(metadataResponse, jsonElement, httpResponse);
    }

    private static JsonNode serializeMetaModel(MetaModel metaModel) {
        Collection<EntityMetadata> allEntityMetadata = metaModel.getAllEntityMetadata();
        ArrayNode entityArray = CommonUtil.json().createArrayNode();
        allEntityMetadata.stream()
                .map(JsonMetaModelSerializer::serializeEntityMetaModel)
                .forEach(entityArray::add);
        return entityArray;
    }

    private static JsonNode serializeEntityMetaModel(EntityMetadata entityMetadata) {
        ObjectMapper json = CommonUtil.json();
        ObjectNode jsonMetadata = json.createObjectNode();
        jsonMetadata.put("name", entityMetadata.getName());
        jsonMetadata.put("hasCompositeKey", entityMetadata.hasCompositeKey());
        jsonMetadata.put("sensitive", entityMetadata.isSensitive());
        ArrayNode fieldArray = json.createArrayNode();
        entityMetadata.getAllFields().stream()
                .map(JsonMetaModelSerializer::serializeFieldMetadata)
                .forEach(fieldArray::add);
        jsonMetadata.set("fields", fieldArray);

        ArrayNode keyArray = json.createArrayNode();
        entityMetadata.getKeyFields().stream()
                .map(JsonMetaModelSerializer::serializeFieldMetadata)
                .forEach(keyArray::add);
        jsonMetadata.set("keyFields", keyArray);

        return jsonMetadata;
    }

    private static JsonNode serializeFieldMetadata(FieldMetadata fieldMetadata) {
        ObjectNode fieldMetadataJson = CommonUtil.json().createObjectNode();
        fieldMetadataJson.put("outputProperty", fieldMetadata.getOutputPropertyName());
        fieldMetadataJson.put("isAssociation", fieldMetadata.isAssociation());

        if (fieldMetadata instanceof KeyFieldMetadata keyFieldMetadata) {
            fieldMetadataJson.put("isKeyField", keyFieldMetadata.isKeyField());
            fieldMetadataJson.put("isKeyGenerated", keyFieldMetadata.shouldGenerateKey());
        }

        if (fieldMetadata.isAssociation()) {
            fieldMetadataJson.put("associatedWith", fieldMetadata.getAssociationMetadata().getName());
            fieldMetadataJson.put("associatedField", fieldMetadata.getAssociatedFieldMetadata().getOutputPropertyName());
        }

        fieldMetadataJson.put("fieldType", fieldMetadata.getValueType().name());
        fieldMetadataJson.put("isNullable", fieldMetadata.isNullable());
        fieldMetadataJson.put("isAssociation", fieldMetadata.isAssociation());
        fieldMetadataJson.put("isCollection", fieldMetadata.isCollection());
        fieldMetadataJson.put("isSerializable", fieldMetadata.isSerializable());
        fieldMetadataJson.put("isDeserializable", fieldMetadata.isDeserializable());
        fieldMetadataJson.put("isUnique", fieldMetadata.isUnique());
        fieldMetadataJson.put("sensitive", fieldMetadata.isSensitive());

        return fieldMetadataJson;
    }
}
