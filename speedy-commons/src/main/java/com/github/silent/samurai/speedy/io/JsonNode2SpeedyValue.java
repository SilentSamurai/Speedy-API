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
import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.*;

public class JsonNode2SpeedyValue {
    public static SpeedyValue fromValueNode(FieldMetadata fieldMetadata, ValueNode jsonNode) throws BadRequestException {
        if (jsonNode.isNull()) {
            return fromNull();
        }
        return switch (fieldMetadata.getValueType()) {
            case BOOL:
                yield fromBool(jsonNode.asBoolean());
            case TEXT:
                yield fromText(jsonNode.asText());
            case INT:
                yield fromInt(jsonNode.asLong());
            case FLOAT:
                yield fromDouble(jsonNode.asDouble());
            case DATE:
                if (!jsonNode.isTextual() || !isDateFormatValid(jsonNode.asText())) {
                    String formatString = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                    String msg = String.format("Date value must be a string with ISO_DATE(%s) format", formatString);
                    throw new BadRequestException(msg);
                }
                LocalDate localDate = LocalDate.parse(jsonNode.asText(), DateTimeFormatter.ISO_DATE);
                yield fromDate(localDate);
            case TIME:
                if (!jsonNode.isTextual() || !isTimeFormatValid(jsonNode.asText())) {
                    String formatString = LocalTime.now().format(DateTimeFormatter.ISO_TIME);
                    String msg = String.format("Time value must be a string with ISO_TIME(%s) format", formatString);
                    throw new BadRequestException(msg);
                }
                LocalTime localTime = LocalTime.parse(jsonNode.asText(), DateTimeFormatter.ISO_TIME);
                yield fromTime(localTime);
            case DATE_TIME:
                if (!jsonNode.isTextual() || !isDateTimeFormatValid(jsonNode.asText())) {
                    String formatString = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
                    String msg = String.format("DateTime value must be a string with ISO_DATE_TIME(%s) format", formatString);
                    throw new BadRequestException(msg);
                }
                LocalDateTime datetime = LocalDateTime.parse(jsonNode.asText(), DateTimeFormatter.ISO_DATE_TIME);
                yield fromDateTime(datetime);
            case ZONED_DATE_TIME:
                if (!jsonNode.isTextual() || !isZonedDateTimeValid(jsonNode.asText())) {
                    String formatString = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    String msg = String.format("ZonedDateTime value must be a string with ISO_ZONED_DATE_TIME(%s) format", formatString);
                    throw new BadRequestException(msg);
                }
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(jsonNode.asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                yield fromZonedDateTime(zonedDateTime);
            case NULL:
                yield fromNull();
            case OBJECT:
            case COLLECTION:
                String msg = String
                        .format("Not able to parse : %s\nfor Field: %s, Value Type: %s ",
                                jsonNode.toPrettyString(),
                                fieldMetadata.getOutputPropertyName(),
                                fieldMetadata.getColumnType());
                throw new BadRequestException(msg);
        };
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
