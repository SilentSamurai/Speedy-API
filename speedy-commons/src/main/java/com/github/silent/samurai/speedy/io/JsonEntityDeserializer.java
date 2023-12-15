package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;

import static com.github.silent.samurai.speedy.utils.SpeedyValueFactory.*;

public class JsonEntityDeserializer {
    public static SpeedyValue fromValueTypePrimitive(FieldMetadata fieldMetadata, JsonNode jsonNode) throws BadRequestException {
        switch (fieldMetadata.getValueType()) {
            case TEXT:
                return fromText(jsonNode.asText());
            case INT:
                return fromInt(jsonNode.asInt());
            case FLOAT:
                return fromDouble(jsonNode.asDouble());
            case DATE:
                if (!jsonNode.isTextual()) {
                    throw new BadRequestException("Date value must be a string");
                }
                String dateValue = jsonNode.asText();
                LocalDate localDate = LocalDate.parse(dateValue, DateTimeFormatter.ISO_DATE);
                return fromDate(localDate);
            case TIME:
                if (!jsonNode.isTextual()) {
                    throw new BadRequestException("Time value must be a string");
                }
                String timeValue = jsonNode.asText();
                LocalTime localTime = LocalTime.parse(timeValue, DateTimeFormatter.ISO_TIME);
                return fromTime(localTime);
            case DATE_TIME:
                if (!jsonNode.isTextual()) {
                    throw new BadRequestException("DateTime value must be a string");
                }
                String datetimeValue = jsonNode.asText();
                LocalDateTime datetime = LocalDateTime.parse(datetimeValue, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                return fromDateTime(datetime);
            default:
                return SpeedyNull.SPEEDY_NULL;
        }
    }

    public static SpeedyEntity fromEntityMetadata(EntityMetadata entityMetadata, ObjectNode jsonNode) throws SpeedyHttpException {
        SpeedyEntity speedyEntity = new SpeedyEntity(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (jsonNode.has(fieldMetadata.getOutputPropertyName()) && !jsonNode.get(fieldMetadata.getOutputPropertyName()).isNull()) {
                JsonNode fieldObject = jsonNode.get(fieldMetadata.getOutputPropertyName());
                SpeedyValue speedyValue = fromFieldMetadata(fieldMetadata, fieldObject);
                speedyEntity.put(fieldMetadata, speedyValue);
            }
        }
        return speedyEntity;
    }

    public static SpeedyValue fromFieldMetadata(FieldMetadata fieldMetadata, JsonNode jsonNode) throws SpeedyHttpException {
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
                    SpeedyValue speedyValue = JavaTypeToSpeedyConverter.convert(JsonNode.class, fieldMetadata.getValueType(), item);
                    collection.add(speedyValue);
                }
                return fromCollection(collection);
            } else {
                return JavaTypeToSpeedyConverter.convert(JsonNode.class, fieldMetadata.getValueType(), jsonNode);
            }
        }
    }


    public static SpeedyEntityKey fromPkJson(EntityMetadata entityMetadata, ObjectNode jsonNode) throws SpeedyHttpException {
        SpeedyEntityKey speedyEntityKey = new SpeedyEntityKey(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getKeyFields()) {
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (jsonNode.has(propertyName)
                    && !jsonNode.get(propertyName).isNull()) {
                JsonNode fieldObject = jsonNode.get(fieldMetadata.getOutputPropertyName());
                SpeedyValue speedyValue = fromFieldMetadata(fieldMetadata, fieldObject);
                speedyEntityKey.put(fieldMetadata, speedyValue);
            } else{
                throw new BadRequestException("Missing key field: " + propertyName);
            }
        }
        return speedyEntityKey;
    }

}
