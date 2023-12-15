package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.io.BasicDeserializer;
import com.github.silent.samurai.speedy.io.JavaTypeToSpeedyConverter;
import com.github.silent.samurai.speedy.io.JsonEntityDeserializer;
import com.github.silent.samurai.speedy.io.Speedy2JavaTypeConverter;
import com.github.silent.samurai.speedy.models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;


public class SpeedyValueFactory {

    public static SpeedyNull fromNull() {
        return SpeedyNull.SPEEDY_NULL;
    }

    public static SpeedyText fromText(String value) {
        return new SpeedyText(value);
    }

    public static SpeedyInt fromInt(Integer value) {
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

    public static SpeedyCollection fromCollection(Collection<SpeedyValue> value) {
        return new SpeedyCollection(value);
    }

    public static SpeedyValue fromJavaTypes(FieldMetadata fieldMetadata, Object instance) throws SpeedyHttpException {
        return JavaTypeToSpeedyConverter.convert(fieldMetadata.getFieldType(), fieldMetadata.getValueType(), instance);
    }

    public static SpeedyValue fromQuotedString(FieldMetadata fieldMetadata, String quotedValue) throws SpeedyHttpException {
        Object instance = BasicDeserializer.quotedStringToPrimitive(quotedValue, fieldMetadata.getFieldType());
        return JavaTypeToSpeedyConverter.convert(fieldMetadata.getFieldType(), fieldMetadata.getValueType(), instance);
    }

    public static SpeedyValue fromJson(FieldMetadata fieldMetadata, JsonNode jsonNode) throws SpeedyHttpException {
        return JsonEntityDeserializer.fromFieldMetadata(fieldMetadata, jsonNode);
    }

    public static SpeedyEntity fromJson(EntityMetadata entityMetadata, ObjectNode jsonNode) throws SpeedyHttpException {
        return JsonEntityDeserializer.fromEntityMetadata(entityMetadata, jsonNode);
    }

    public static SpeedyEntityKey fromPkJson(EntityMetadata entityMetadata, ObjectNode jsonNode) throws SpeedyHttpException {
        return JsonEntityDeserializer.fromPkJson(entityMetadata, jsonNode);
    }


    public static <T> T toJavaType(FieldMetadata fieldMetadata, SpeedyValue speedyValue) throws SpeedyHttpException {
        return (T) Speedy2JavaTypeConverter.convert(speedyValue, fieldMetadata.getFieldType());
    }

}
