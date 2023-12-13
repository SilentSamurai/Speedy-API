package com.github.silent.samurai.speedy.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedList;

import static com.github.silent.samurai.speedy.models.SpeedyValueFactory.*;

public class JsonEntityDeserializer {

    private final ObjectNode entityJson;
    private final EntityMetadata entityMetadata;

    public JsonEntityDeserializer(ObjectNode entityJson, EntityMetadata entityMetadata) {
        this.entityJson = entityJson;
        this.entityMetadata = entityMetadata;
    }

    public static SpeedyValue fromValueTypePrimitive(ValueType type, JsonNode jsonNode) {
        switch (type) {
            case TEXT:
                return fromText(jsonNode.asText());
            case INT:
                return fromInt(jsonNode.asInt());
            case FLOAT:
                return fromDouble(jsonNode.asDouble());
            case DATE:
                String dateValue = jsonNode.asText();
                return fromDate(LocalDate.parse(dateValue));
            case TIME:
                String timeValue = jsonNode.asText();
                return fromTime(LocalTime.parse(timeValue));
            case DATE_TIME:
                String datetimeValue = jsonNode.asText();
                return fromDateTime(LocalDateTime.parse(datetimeValue));
            default:
                return SpeedyNull.SPEEDY_NULL;
        }
    }

    public static SpeedyEntity fromEntityMetadata(EntityMetadata entityMetadata, ObjectNode jsonNode) {
        SpeedyEntity speedyEntity = new SpeedyEntity(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (jsonNode.has(fieldMetadata.getOutputPropertyName())) {
                JsonNode fieldObject = jsonNode.get(fieldMetadata.getOutputPropertyName());
                SpeedyValue speedyValue = fromFieldMetadata(fieldMetadata, fieldObject);
                speedyEntity.put(fieldMetadata, speedyValue);
            }
        }
        return speedyEntity;
    }

    public static SpeedyValue fromFieldMetadata(FieldMetadata fieldMetadata, JsonNode jsonNode) {
        if (fieldMetadata.isAssociation()) {
            if (fieldMetadata.isCollection()) {
                ArrayNode arrayNode = (ArrayNode) jsonNode;
                Collection<SpeedyValue> collection = new LinkedList<>();
                for (JsonNode item : arrayNode) {
                    SpeedyValue speedyValue = fromEntityMetadata(fieldMetadata.getAssociationMetadata(), (ObjectNode) item);
                    collection.add(speedyValue);
                }
                return fromCollection(collection);
            } else {
                return fromEntityMetadata(fieldMetadata.getAssociationMetadata(), (ObjectNode) jsonNode);
            }
        } else {
            if (fieldMetadata.isCollection()) {
                ArrayNode arrayNode = (ArrayNode) jsonNode;
                Collection<SpeedyValue> collection = new LinkedList<>();
                for (JsonNode item : arrayNode) {
                    SpeedyValue speedyValue = fromValueTypePrimitive(fieldMetadata.getValueType(), item);
                    collection.add(speedyValue);
                }
                return fromCollection(collection);
            } else {
                return fromValueTypePrimitive(fieldMetadata.getValueType(), jsonNode);
            }
        }
    }

    public SpeedyEntity deserialize() throws Exception {
        return fromEntityMetadata(entityMetadata, entityJson);
    }


}
