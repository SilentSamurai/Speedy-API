package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.ThrowingBiFunction;
import com.github.silent.samurai.speedy.models.*;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.github.silent.samurai.speedy.utils.SpeedyValueFactory.*;

public class JavaTypeToSpeedyConverter {
    private static final Map<String, ThrowingBiFunction<Object, ValueType, SpeedyValue, SpeedyHttpException>> converters = new HashMap<>();

    public static <T> ThrowingBiFunction<Object, ValueType, SpeedyValue, SpeedyHttpException> get(ValueType valueType, Class<T> clazz) {
        String key = clazz.getName() + valueType.name();
        return converters.get(key);
    }

    public static boolean has(ValueType valueType, Class<?> clazz) {
        String key = clazz.getName() + valueType.name();
        return converters.containsKey(key);
    }

    public static void put(ValueType valueType, Class<?> clazz,
                           ThrowingBiFunction<Object, ValueType, SpeedyValue, SpeedyHttpException> lambda) {
        String key = clazz.getName() + valueType.name();
        converters.put(key, lambda);
    }

    static {
        initConverters();
    }

    public static <T> SpeedyValue convert(Class<T> clazz, ValueType valueType, Object instance) throws SpeedyHttpException {
        if (instance == null || !has(valueType, clazz)) {
            return SpeedyNull.SPEEDY_NULL;
        }
        return get(valueType, clazz).apply(instance, valueType);
    }

    private static void initConverters() {
        put(ValueType.TEXT, String.class, (instance, valueType) -> {
            return new SpeedyText((String) instance);
        });
        put(ValueType.INT, int.class, (instance, valueType) -> {
            return new SpeedyInt((Integer) instance);
        });
        put(ValueType.INT, Integer.class, (instance, valueType) -> {
            return new SpeedyInt((Integer) instance);
        });
        put(ValueType.INT, long.class, (instance, valueType) -> {
            return new SpeedyInt((Integer) instance);
        });
        put(ValueType.INT, Long.class, (instance, valueType) -> {
            return new SpeedyInt((Integer) instance);
        });
        put(ValueType.FLOAT, float.class, (instance, valueType) -> {
            return new SpeedyDouble((Double) instance);
        });
        put(ValueType.FLOAT, Float.class, (instance, valueType) -> {
            return new SpeedyDouble((Double) instance);
        });
        put(ValueType.FLOAT, double.class, (instance, valueType) -> {
            return new SpeedyDouble((Double) instance);
        });
        put(ValueType.FLOAT, Double.class, (instance, valueType) -> {
            return new SpeedyDouble((Double) instance);
        });
        put(ValueType.BOOL, boolean.class, (instance, valueType) -> {
            return new SpeedyBoolean((Boolean) instance);
        });
        put(ValueType.BOOL, Boolean.class, (instance, valueType) -> {
            return new SpeedyBoolean((Boolean) instance);
        });
        put(ValueType.DATE, java.sql.Date.class, (instance, valueType) -> {
            java.sql.Date sqlDate = (java.sql.Date) instance;
            return new SpeedyDate(sqlDate.toLocalDate());
        });
        put(ValueType.DATE, Date.class, (instance, valueType) -> {
            Date kdate = (Date) instance;
            LocalDate localDate = kdate.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
            return new SpeedyDate(localDate);
        });
        put(ValueType.DATE, Instant.class, (instance, valueType) -> {
            Instant kdate = (Instant) instance;
            LocalDate localDate = kdate.atZone(ZoneId.of("UTC")).toLocalDate();
            return new SpeedyDate(localDate);
        });
        put(ValueType.DATE_TIME, Instant.class, (instance, valueType) -> {
            Instant kdate = (Instant) instance;
            LocalDateTime localDateTime = kdate.atZone(ZoneId.of("UTC")).toLocalDateTime();
            return new SpeedyDateTime(localDateTime);
        });
//        put(ValueType.TIME, Instant.class, (instance, valueType) -> {
//            Instant kdate = (Instant) instance;
//            LocalDateTime localDateTime = kdate.atZone(ZoneId.systemDefault()).toLocalDateTime();
//            return new SpeedyDateTime(localDateTime);
//        });
        put(ValueType.DATE, LocalDate.class, (instance, valueType) -> {
            LocalDate kdate = (LocalDate) instance;
            return new SpeedyDate(kdate);
        });
        put(ValueType.DATE_TIME, LocalDateTime.class, (instance, valueType) -> {
            LocalDateTime kdate = (LocalDateTime) instance;
            return new SpeedyDateTime(kdate);
        });
        put(ValueType.DATE_TIME, Timestamp.class, (instance, valueType) -> {
            Timestamp kdate = (Timestamp) instance;
            LocalDateTime localDateTime = kdate.toLocalDateTime();
            return new SpeedyDateTime(localDateTime);
        });

        // Json to SpeedyValue
        put(ValueType.TEXT, JsonNode.class, (instance, valueType) -> {
            JsonNode jsonNode = (JsonNode) instance;
            return new SpeedyText(jsonNode.asText());
        });
        put(ValueType.INT, JsonNode.class, (instance, valueType) -> {
            JsonNode jsonNode = (JsonNode) instance;
            return new SpeedyInt(jsonNode.asInt());
        });
        put(ValueType.FLOAT, JsonNode.class, (instance, valueType) -> {
            JsonNode jsonNode = (JsonNode) instance;
            return new SpeedyDouble(jsonNode.asDouble());
        });
        put(ValueType.DATE, JsonNode.class, (instance, valueType) -> {
            JsonNode jsonNode = (JsonNode) instance;
            String dateValue = jsonNode.asText();
            LocalDate localDate = LocalDate.parse(dateValue, DateTimeFormatter.ISO_DATE);
            return new SpeedyDate(localDate);
        });
        put(ValueType.TIME, JsonNode.class, (instance, valueType) -> {
            JsonNode jsonNode = (JsonNode) instance;
            String timeValue = jsonNode.asText();
            LocalTime localTime = LocalTime.parse(timeValue, DateTimeFormatter.ISO_TIME);
            return new SpeedyTime(localTime);
        });
        put(ValueType.DATE_TIME, JsonNode.class, (instance, valueType) -> {
            JsonNode jsonNode = (JsonNode) instance;
            String datetimeValue = jsonNode.asText();
            LocalDateTime datetime = LocalDateTime.parse(datetimeValue, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            return new SpeedyDateTime(datetime);
        });

    }
}
