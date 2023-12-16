package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.io.JavaType2SpeedyValue;
import com.github.silent.samurai.speedy.io.Json2Speedy;
import com.github.silent.samurai.speedy.io.Speedy2JavaTypeConverter;
import com.github.silent.samurai.speedy.models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collection;


public class SpeedyValueFactory {

    public static SpeedyNull fromNull() {
        return SpeedyNull.SPEEDY_NULL;
    }

    public static SpeedyBoolean fromBool(Boolean value) {
        return new SpeedyBoolean(value);
    }

    public static SpeedyText fromText(String value) {
        return new SpeedyText(value);
    }

    public static SpeedyInt fromInt(Long value) {
        return new SpeedyInt(value);
    }

    public static SpeedyDouble fromDouble(Double value) {
        return new SpeedyDouble(value);
    }

    public static SpeedyDate fromDate(LocalDate value) {
        return new SpeedyDate(value);
    }

    public static SpeedyTime fromTime(LocalTime value) {
        return new SpeedyTime(value);
    }

    public static SpeedyDateTime fromDateTime(LocalDateTime value) {
        return new SpeedyDateTime(value);
    }

    public static SpeedyZonedDateTime fromZonedDateTime(ZonedDateTime zonedDateTime) {
        return new SpeedyZonedDateTime(zonedDateTime);
    }

    public static SpeedyCollection fromCollection(Collection<SpeedyValue> value) {
        return new SpeedyCollection(value);
    }

    public static <T> SpeedyValue fromJavaTypes(Class<T> clazz, ValueType valueType, Object instance) throws SpeedyHttpException {
        return JavaType2SpeedyValue.convert(clazz, valueType, instance);
    }

    public static SpeedyValue fromJavaTypes(FieldMetadata fieldMetadata, Object instance) throws SpeedyHttpException {
        return JavaType2SpeedyValue.convert(fieldMetadata.getFieldType(), fieldMetadata.getValueType(), instance);
    }

    public static SpeedyValue fromQuotedString(FieldMetadata fieldMetadata, String quotedValue) throws SpeedyHttpException {
        try {
            JsonNode jsonNode = CommonUtil.json().readTree(quotedValue);
            return fromJsonNode(fieldMetadata, jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static SpeedyValue fromJsonValue(FieldMetadata fieldMetadata, ValueNode jsonNode) throws SpeedyHttpException {
        return Json2Speedy.fromValueNode(fieldMetadata, jsonNode);
    }

    public static SpeedyValue fromJsonNode(FieldMetadata fieldMetadata, JsonNode jsonNode) throws SpeedyHttpException {
        return Json2Speedy.fromFieldMetadata(fieldMetadata, jsonNode);
    }

    public static SpeedyEntity fromJsonObject(EntityMetadata entityMetadata, ObjectNode jsonNode) throws SpeedyHttpException {
        return Json2Speedy.fromEntityMetadata(entityMetadata, jsonNode);
    }

    public static SpeedyEntityKey fromPkJson(EntityMetadata entityMetadata, ObjectNode jsonNode) throws SpeedyHttpException {
        return Json2Speedy.fromPkJson(entityMetadata, jsonNode);
    }

    public static <T> T toJavaType(FieldMetadata fieldMetadata, SpeedyValue speedyValue) throws SpeedyHttpException {
        return (T) Speedy2JavaTypeConverter.convert(speedyValue, fieldMetadata.getFieldType());
    }

}
