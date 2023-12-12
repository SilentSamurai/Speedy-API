package com.github.silent.samurai.speedy.io;

import com.github.silent.samurai.speedy.interfaces.ThrowingFunction;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Speedy2JavaTypeConverter {

    private static final Map<Class<?>, ThrowingFunction<SpeedyValue, Object, Exception>> converters = new HashMap<>();

    static {
        initConverters();
    }

    public static <T> T convert(SpeedyValue value, Class<T> clazz) throws Exception {
        if (!converters.containsKey(clazz) || value instanceof SpeedyNull) {
            return null;
        }
        return clazz.cast(converters.get(clazz).apply(value));
    }

    private static void initConverters() {
//        converters.put(null, value -> return null);
        converters.put(String.class, value -> {
            return ((SpeedyText) value).getValue();
        });
        converters.put(int.class, Speedy2JavaTypeConverter::toInteger);
        converters.put(Integer.class, Speedy2JavaTypeConverter::toInteger);
        converters.put(long.class, Speedy2JavaTypeConverter::toLong);
        converters.put(Long.class, Speedy2JavaTypeConverter::toLong);
        converters.put(float.class, Speedy2JavaTypeConverter::toFloat);
        converters.put(Float.class, Speedy2JavaTypeConverter::toFloat);
        converters.put(double.class, Speedy2JavaTypeConverter::toDouble);
        converters.put(Double.class, Speedy2JavaTypeConverter::toDouble);
        converters.put(boolean.class, Speedy2JavaTypeConverter::toBoolean);
        converters.put(Boolean.class, Speedy2JavaTypeConverter::toBoolean);
        converters.put(java.sql.Date.class, speedyValue -> {
            SpeedyDate speedyDate = (SpeedyDate) speedyValue;
            return java.sql.Date.valueOf(speedyDate.getValue());
        });
        converters.put(Date.class, speedyValue -> {
            try {
                SpeedyDate speedyDate = (SpeedyDate) speedyValue;
                Instant instant = speedyDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant();
                return Date.from(instant);
            } catch (Exception e) {
                return null;
            }
        });
        converters.put(Instant.class, speedyValue -> {
            SpeedyDate speedyDate = (SpeedyDate) speedyValue;
            return speedyDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant();
        });
        converters.put(LocalDate.class, speedyValue -> {
            SpeedyDate speedyDate = (SpeedyDate) speedyValue;
            return speedyDate.getValue();
        });
        converters.put(LocalDateTime.class, speedyValue -> {
            SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
            return speedyDateTime.getValue();
        });
        converters.put(Timestamp.class, speedyValue -> {
            SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
            return Timestamp.valueOf(speedyDateTime.getValue());
        });
    }

    static Integer toInteger(SpeedyValue speedyValue) {
        SpeedyInt speedyInt = (SpeedyInt) speedyValue;
        return speedyInt.getValue();
    }

    static Long toLong(SpeedyValue speedyValue) {
        SpeedyInt speedyInt = (SpeedyInt) speedyValue;
        return speedyInt.getValue().longValue();
    }

    static Float toFloat(SpeedyValue speedyValue) {
        SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
        return speedyDouble.getValue().floatValue();
    }

    static Double toDouble(SpeedyValue speedyValue) {
        SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
        return speedyDouble.getValue();
    }

    static Boolean toBoolean(SpeedyValue speedyValue) {
        SpeedyBoolean speedyBoolean = (SpeedyBoolean) speedyValue;
        return speedyBoolean.getValue();
    }

}
