package com.github.silent.samurai.speedy.io;

import com.github.silent.samurai.speedy.interfaces.ThrowingFunction;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JavaTypeToSpeedyConverter {
    private static final Map<Class<?>, ThrowingFunction<Object, SpeedyValue, Exception>> converters = new HashMap<>();

    static {
        initConverters();
    }

    public static <T> SpeedyValue convert(Object instance, Class<T> clazz) throws Exception {
        if (instance == null || !converters.containsKey(clazz)) {
            return SpeedyNull.SPEEDY_NULL;
        }
        return converters.get(clazz).apply(instance);
    }

    private static void initConverters() {
//        converters.put(null, instance -> new SpeedyNull());
        converters.put(String.class, instance -> new SpeedyText((String) instance));
        converters.put(int.class, instance -> {
            return new SpeedyInt((Integer) instance);
        });
        converters.put(Integer.class, instance -> {
            return new SpeedyInt((Integer) instance);
        });
        converters.put(long.class, instance -> {
            return new SpeedyInt((Integer) instance);
        });
        converters.put(Long.class, instance -> {
            return new SpeedyInt((Integer) instance);
        });
        converters.put(float.class, instance -> {
            return new SpeedyDouble((Double) instance);
        });
        converters.put(Float.class, instance -> {
            return new SpeedyDouble((Double) instance);
        });
        converters.put(double.class, instance -> {
            return new SpeedyDouble((Double) instance);
        });
        converters.put(Double.class, instance -> {
            return new SpeedyDouble((Double) instance);
        });
        converters.put(boolean.class, instance -> {
            return new SpeedyBoolean((Boolean) instance);
        });
        converters.put(Boolean.class, instance -> {
            return new SpeedyBoolean((Boolean) instance);
        });
        converters.put(java.sql.Date.class, instance -> {
            java.sql.Date sqlDate = (java.sql.Date) instance;
            return new SpeedyDate(sqlDate.toLocalDate());
        });
        converters.put(Date.class, instance -> {
            Date kdate = (Date) instance;
            LocalDate localDate = kdate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return new SpeedyDate(localDate);
        });
        converters.put(Instant.class, instance -> {
            Instant kdate = (Instant) instance;
            LocalDateTime localDate = kdate.atZone(ZoneId.systemDefault()).toLocalDateTime();
            return new SpeedyDateTime(localDate);
        });
        converters.put(LocalDate.class, instance -> {
            LocalDate kdate = (LocalDate) instance;
            return new SpeedyDate(kdate);
        });
        converters.put(LocalDateTime.class, instance -> {
            LocalDateTime kdate = (LocalDateTime) instance;
            return new SpeedyDateTime(kdate);
        });
        converters.put(Timestamp.class, instance -> {
            Timestamp kdate = (Timestamp) instance;
            LocalDateTime localDateTime = kdate.toLocalDateTime();
            return new SpeedyDateTime(localDateTime);
        });

    }
}
