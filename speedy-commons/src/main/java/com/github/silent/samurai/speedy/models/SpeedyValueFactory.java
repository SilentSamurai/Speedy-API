package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import com.github.silent.samurai.speedy.io.JavaTypeToSpeedyConverter;
import com.github.silent.samurai.speedy.io.Speedy2JavaTypeConverter;

import java.time.*;
import java.time.format.DateTimeFormatter;
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

    public static <T> SpeedyValue basicToSpeedyValue(Class<T> tClass, Object instance) throws Exception {
        return JavaTypeToSpeedyConverter.convert(instance, tClass);
    }

    public static <T> T speedyValueToJavaType(SpeedyValue speedyValue, Class<T> clazz) throws Exception {
        return Speedy2JavaTypeConverter.convert(speedyValue, clazz);
    }


}