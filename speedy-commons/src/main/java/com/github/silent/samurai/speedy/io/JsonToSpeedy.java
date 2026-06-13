package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.mappings.JsonRegistry;
import com.github.silent.samurai.speedy.models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;

import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.*;

/// # JsonToSpeedy
///
/// Converts Jackson {@link com.fasterxml.jackson.databind.JsonNode} trees into
/// {@link com.github.silent.samurai.speedy.interfaces.SpeedyValue} instances.
/// Uses the injected {@link JsonRegistry} for type-safe JSON decoding of all
/// non-enum, non-collection scalar values.
///
/// ENUM and ENUM_ORD types are validated directly against the field metadata
/// and remain handled inline rather than through the registry.
///
/// @see JsonRegistry
/// @see SpeedyToJson
public class JsonToSpeedy {

    /// The JSON registry used for decoding scalar JSON nodes into SpeedyValue instances.
    private final JsonRegistry jsonRegistry;

    /// Creates a converter backed by the given JSON registry.
    ///
    /// @param jsonRegistry the registry to use for JSON decoding
    public JsonToSpeedy(JsonRegistry jsonRegistry) {
        this.jsonRegistry = jsonRegistry;
    }

    public SpeedyValue fromValueNode(FieldMetadata fieldMetadata, ValueNode jsonNode) throws BadRequestException {
        if (jsonNode.isNull()) {
            return SpeedyNull.SPEEDY_NULL;
        }
        return switch (fieldMetadata.getValueType()) {
            case ENUM -> {
                if (!jsonNode.isTextual())
                    throw new BadRequestException("expected string for enum field " + fieldMetadata.getOutputPropertyName());
                yield new SpeedyEnum(jsonNode.asText(), fieldMetadata);
            }
            case ENUM_ORD -> {
                if (!jsonNode.isNumber())
                    throw new BadRequestException("expected number for ordinal enum field " + fieldMetadata.getOutputPropertyName());
                yield new SpeedyEnum(jsonNode.asLong(), fieldMetadata);
            }
            case DATE -> {
                if (!jsonNode.isTextual() || !isDateFormatValid(jsonNode.asText())) {
                    String formatString = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                    String msg = String.format("Date value must be a string with ISO_DATE(%s) format", formatString);
                    throw new BadRequestException(msg);
                }
                yield decode(ValueType.DATE, jsonNode.asText());
            }
            case TIME -> {
                if (!jsonNode.isTextual() || !isTimeFormatValid(jsonNode.asText())) {
                    String formatString = LocalTime.now().format(DateTimeFormatter.ISO_TIME);
                    String msg = String.format("Time value must be a string with ISO_TIME(%s) format", formatString);
                    throw new BadRequestException(msg);
                }
                yield decode(ValueType.TIME, jsonNode.asText());
            }
            case DATE_TIME -> {
                if (!jsonNode.isTextual() || !isDateTimeFormatValid(jsonNode.asText())) {
                    String formatString = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
                    String msg = String.format("DateTime value must be a string with ISO_DATE_TIME(%s) format", formatString);
                    throw new BadRequestException(msg);
                }
                yield decode(ValueType.DATE_TIME, jsonNode.asText());
            }
            case ZONED_DATE_TIME -> {
                if (!jsonNode.isTextual() || !isZonedDateTimeValid(jsonNode.asText())) {
                    String formatString = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    String msg = String.format("ZonedDateTime value must be a string with ISO_ZONED_DATE_TIME(%s) format", formatString);
                    throw new BadRequestException(msg);
                }
                yield decode(ValueType.ZONED_DATE_TIME, jsonNode.asText());
            }
            case BOOL -> decode(ValueType.BOOL, jsonNode.asBoolean());
            case TEXT -> decode(ValueType.TEXT, jsonNode.asText());
            case INT -> decode(ValueType.INT, jsonNode.asLong());
            case FLOAT -> decode(ValueType.FLOAT, jsonNode.asDouble());
            case NULL -> SpeedyNull.SPEEDY_NULL;
            case OBJECT, COLLECTION -> {
                String msg = String
                        .format("Not able to parse : %s\nfor Field: %s, Value Type: %s ",
                                jsonNode.toPrettyString(),
                                fieldMetadata.getOutputPropertyName(),
                                fieldMetadata.getColumnType());
                throw new BadRequestException(msg);
            }
        };
    }

    /// Decodes a raw JSON value into a SpeedyValue using the {@link JsonRegistry}.
    /// The registry looks up the codec by {@code (ValueType, raw.getClass())} and
    /// verifies the type at runtime via {@link Codec#safeDecode(Object)}.
    private SpeedyValue decode(ValueType vt, Object rawValue) throws BadRequestException {
        return jsonRegistry.decode(vt, rawValue);
    }

    public SpeedyEntity fromEntityMetadata(EntityMetadata entityMetadata, ObjectNode jsonNode) throws SpeedyHttpException {
        SpeedyEntity speedyEntity = new SpeedyEntity(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (fieldMetadata.isDeserializable()
                    && jsonNode.has(fieldMetadata.getOutputPropertyName())
                    && !jsonNode.get(fieldMetadata.getOutputPropertyName()).isNull()) {
                JsonNode fieldObject = jsonNode.get(fieldMetadata.getOutputPropertyName());
                SpeedyValue speedyValue = fromFieldMetadata(fieldMetadata, fieldObject);
                speedyEntity.put(fieldMetadata, speedyValue);
            }
        }
        return speedyEntity;
    }

    public SpeedyValue fromFieldMetadata(FieldMetadata fieldMetadata, JsonNode jsonNode) throws SpeedyHttpException {
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
                return new SpeedyCollection(collection);
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
                return new SpeedyCollection(collection);
            } else {
                if (!jsonNode.isValueNode()) {
                    throw new BadRequestException("Field " + fieldMetadata.getOutputPropertyName() + " must be a value");
                }
                return fromValueNode(fieldMetadata, (ValueNode) jsonNode);
            }
        }
    }


    public SpeedyEntityKey fromPkJson(EntityMetadata entityMetadata, ObjectNode jsonNode) throws SpeedyHttpException {
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
