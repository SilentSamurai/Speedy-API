package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;

import static com.github.silent.samurai.speedy.utils.SpeedyValueFactory.*;

public class Json2Speedy {
    public static SpeedyValue fromValueNode(FieldMetadata fieldMetadata, ValueNode jsonNode) throws BadRequestException {
        if (jsonNode.isNull()) {
            return fromNull();
        }
        switch (fieldMetadata.getValueType()) {
            case BOOL:
                return fromBool(jsonNode.asBoolean());
            case TEXT:
                return fromText(jsonNode.asText());
            case INT:
                return fromInt(jsonNode.asLong());
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
                LocalDateTime datetime = LocalDateTime.parse(datetimeValue, DateTimeFormatter.ISO_DATE_TIME);
                return fromDateTime(datetime);
            case ZONED_DATE_TIME:
                if (!jsonNode.isTextual()) {
                    throw new BadRequestException("ZonedDateTime value must be a string");
                }
                String zonedDateTimeValue = jsonNode.asText();
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(zonedDateTimeValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                return fromZonedDateTime(zonedDateTime);
            case NULL:
                return fromNull();
            case OBJECT:
            case COLLECTION:
                throw new BadRequestException("Field " + fieldMetadata.getOutputPropertyName() + " must be a value");
        }
        throw new BadRequestException("Field " + fieldMetadata.getOutputPropertyName() + " must be a value");
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
                if (!jsonNode.isArray()) {
                    throw new BadRequestException("Field " + fieldMetadata.getOutputPropertyName() + " must be an array");
                }
                ArrayNode arrayNode = (ArrayNode) jsonNode;
                Collection<SpeedyValue> collection = new LinkedList<>();
                for (JsonNode item : arrayNode) {
                    if (!item.isObject()) {
                        throw new BadRequestException("Field " + fieldMetadata.getOutputPropertyName() + " must be an object");
                    }
                    SpeedyValue speedyValue = fromEntityMetadata(fieldMetadata.getAssociationMetadata(), (ObjectNode) item);
                    collection.add(speedyValue);
                }
                return fromCollection(collection);
            } else {
                if (!jsonNode.isObject()) {
                    throw new BadRequestException("Field " + fieldMetadata.getOutputPropertyName() + " must be an object");
                }
                return fromEntityMetadata(fieldMetadata.getAssociationMetadata(), (ObjectNode) jsonNode);
            }
        } else {
            if (fieldMetadata.isCollection()) {
                if (!jsonNode.isArray()) {
                    throw new BadRequestException("Field " + fieldMetadata.getOutputPropertyName() + " must be an array");
                }
                ArrayNode arrayNode = (ArrayNode) jsonNode;
                Collection<SpeedyValue> collection = new LinkedList<>();
                for (JsonNode item : arrayNode) {
                    if (!item.isValueNode()) {
                        throw new BadRequestException("Field " + fieldMetadata.getOutputPropertyName() + " must be a value");
                    }
                    SpeedyValue speedyValue = fromValueNode(fieldMetadata, (ValueNode) item);
                    collection.add(speedyValue);
                }
                return fromCollection(collection);
            } else {
                if (!jsonNode.isValueNode()) {
                    throw new BadRequestException("Field " + fieldMetadata.getOutputPropertyName() + " must be a value");
                }
                return fromValueNode(fieldMetadata, (ValueNode) jsonNode);
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
                if (!fieldObject.isValueNode()) {
                    throw new BadRequestException("Key field " + propertyName + " must be a value");
                }
                SpeedyValue speedyValue = fromValueNode(fieldMetadata, (ValueNode) fieldObject);
                speedyEntityKey.put(fieldMetadata, speedyValue);
            } else {
                throw new BadRequestException("Missing key field: " + propertyName);
            }
        }
        return speedyEntityKey;
    }

}
