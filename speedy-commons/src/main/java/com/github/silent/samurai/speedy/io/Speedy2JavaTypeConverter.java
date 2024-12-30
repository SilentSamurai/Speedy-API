package com.github.silent.samurai.speedy.io;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.ThrowingBiFunction;
import com.github.silent.samurai.speedy.models.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Speedy2JavaTypeConverter {

    private static final Map<String, ThrowingBiFunction<SpeedyValue, ValueType, Object, SpeedyHttpException>> converters = new HashMap<>();

    static {
        initConverters();
    }


    public static <T> ThrowingBiFunction<SpeedyValue, ValueType, Object, SpeedyHttpException> get(ValueType valueType, Class<T> clazz) {
        String key = clazz.getName() + valueType.name();
        return converters.get(key);
    }

    public static boolean has(ValueType valueType, Class<?> clazz) {
        String key = clazz.getName() + valueType.name();
        return converters.containsKey(key);
    }

    public static <T> void put(ValueType valueType, Class<T> clazz,
                               ThrowingBiFunction<SpeedyValue, ValueType, T, SpeedyHttpException> converter) {
        String key = clazz.getName() + valueType.name();
        converters.put(key, (ThrowingBiFunction<SpeedyValue, ValueType, Object, SpeedyHttpException>) converter);
    }

    private static void initConverters() {

        put(ValueType.TEXT, String.class, (speedyValue, type) -> {
            return ((SpeedyText) speedyValue).getValue();
        });
        put(ValueType.TEXT, UUID.class, (speedyValue, type) -> {
            return UUID.fromString(speedyValue.asText());
        });
        put(ValueType.INT, int.class, (speedyValue, type) -> {
            SpeedyInt speedyInt = (SpeedyInt) speedyValue;
            return speedyInt.getValue().intValue();
        });
        put(ValueType.INT, Integer.class, (speedyValue, type) -> {
            SpeedyInt speedyInt = (SpeedyInt) speedyValue;
            return speedyInt.getValue().intValue();
        });

        put(ValueType.INT, long.class, (speedyValue, type) -> {
            SpeedyInt speedyInt = (SpeedyInt) speedyValue;
            return speedyInt.getValue();
        });
        put(ValueType.INT, Long.class, (speedyValue, type) -> {
            SpeedyInt speedyInt = (SpeedyInt) speedyValue;
            return speedyInt.getValue();
        });
        put(ValueType.FLOAT, float.class, (speedyValue, type) -> {
            SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
            return speedyDouble.getValue().floatValue();
        });
        put(ValueType.FLOAT, Float.class, (speedyValue, type) -> {
            SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
            return speedyDouble.getValue().floatValue();
        });
        put(ValueType.FLOAT, double.class, (speedyValue, type) -> {
            SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
            return speedyDouble.getValue();
        });
        put(ValueType.FLOAT, Double.class, (speedyValue, type) -> {
            SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
            return speedyDouble.getValue();
        });
        put(ValueType.BOOL, boolean.class, (speedyValue, type) -> {
            SpeedyBoolean speedyBoolean = (SpeedyBoolean) speedyValue;
            return speedyBoolean.getValue();
        });
        put(ValueType.BOOL, Boolean.class, (speedyValue, type) -> {
            SpeedyBoolean speedyBoolean = (SpeedyBoolean) speedyValue;
            return speedyBoolean.getValue();
        });
        put(ValueType.DATE, java.sql.Date.class, (speedyValue, type) -> {
            SpeedyDate speedyDate = (SpeedyDate) speedyValue;
            return java.sql.Date.valueOf(speedyDate.getValue());
        });
        put(ValueType.DATE, Date.class, (speedyValue, type) -> {
            SpeedyDate speedyDate = (SpeedyDate) speedyValue;
            Instant instant = speedyDate.getValue().atStartOfDay(ZoneId.of("UTC")).toInstant();
            return Date.from(instant);
        });
        put(ValueType.DATE, LocalDate.class, (speedyValue, type) -> {
            SpeedyDate speedyDate = (SpeedyDate) speedyValue;
            return speedyDate.asDate();
        });
        put(ValueType.TIME, LocalTime.class, (speedyValue, type) -> {
            SpeedyTime speedyTime = (SpeedyTime) speedyValue;
            return speedyTime.asTime();
        });
//        put(ValueType.TIME, Instant.class, (speedyValue, type) -> {
//            SpeedyTime speedyTime = (SpeedyTime) speedyValue;
//            // convert localTime to Instant object
//            return speedyTime.getValue();
//        });
        put(ValueType.DATE_TIME, Instant.class, (speedyValue, type) -> {
            SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
            return speedyDateTime.getValue().atZone(ZoneId.of("UTC")).toInstant();
        });
        put(ValueType.DATE_TIME, LocalDate.class, (speedyValue, type) -> {
            SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
            return speedyDateTime.getValue().toLocalDate();
        });
        put(ValueType.DATE_TIME, LocalDateTime.class, (speedyValue, type) -> {
            SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
            return speedyDateTime.getValue();
        });
        put(ValueType.DATE_TIME, Timestamp.class, (speedyValue, type) -> {
            SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
            return Timestamp.valueOf(speedyDateTime.getValue());
        });
        put(ValueType.ZONED_DATE_TIME, ZonedDateTime.class, (speedyValue, type) -> {
            SpeedyZonedDateTime speedyDateTime = (SpeedyZonedDateTime) speedyValue;
            return speedyDateTime.asZonedDateTime();
        });

        put(ValueType.ZONED_DATE_TIME, Instant.class, (speedyValue, type) -> {
            SpeedyZonedDateTime speedyDateTime = (SpeedyZonedDateTime) speedyValue;
            return speedyDateTime.asZonedDateTime().toInstant();
        });
    }

    public static <T> T convert(SpeedyValue value, Class<T> clazz) throws SpeedyHttpException {
        ValueType valueType = value.getValueType();
        if (!has(valueType, clazz) || value instanceof SpeedyNull) {
            return null;
        }
        return clazz.cast(get(valueType, clazz).apply(value, valueType));
    }

    public static <T> T convert(SpeedyValue speedyValue, ValueType valueType) {
        switch (valueType) {
            case NULL:
                return null;
            case BOOL:
                if (speedyValue.isBoolean()) {
                    return (T) speedyValue.asBoolean();
                }
            case TEXT:
                if (speedyValue.isText()) {
                    return (T) speedyValue.asText();
                }
            case INT:
                if (speedyValue.isInt()) {
                    return (T) speedyValue.asInt();
                }
            case FLOAT:
                if (speedyValue.isDouble()) {
                    return (T) speedyValue.asDouble();
                }
            case DATE:
                if (speedyValue.isDate()) {
                    return (T) speedyValue.asDate();
                }
            case TIME:
                if (speedyValue.isTime()) {
                    return (T) speedyValue.asTime();
                }
            case DATE_TIME:
                if (speedyValue.isDateTime()) {
                    return (T) speedyValue.asDateTime();
                }
            case ZONED_DATE_TIME:
                if (speedyValue.isZonedDateTime()) {
                    return (T) speedyValue.asZonedDateTime();
                }
            case OBJECT:
            case COLLECTION:
            default:
                throw new ConversionException(
                        String.format("Cannot convert %s to %s", speedyValue, valueType.name())
                );
        }
    }
}
