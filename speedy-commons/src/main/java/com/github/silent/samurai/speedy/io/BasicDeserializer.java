package com.github.silent.samurai.speedy.io;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.ThrowingFunction;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BasicDeserializer {

    private static final Map<Class<?>, ThrowingFunction<String, Object, Exception>> converters = new HashMap<>();

    static {
        initConverters();
    }

    public static <T> T quotedStringToPrimitive(String value, Class<T> type) throws BadRequestException {
        try {
            value = value.replaceAll("['|\"]", "");
            Object obj = stringToBasic(value, type);
            if (obj != null && CommonUtil.isAssignableClass(obj.getClass(), type)) {
                return (T) obj;
            }
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
        return null;
    }

    public static <T> T stringToPrimitive(String value, Class<T> type) throws BadRequestException {
        try {
            Object obj = stringToBasic(value, type);
            if (obj != null && CommonUtil.isAssignableClass(obj.getClass(), type)) {
                return (T) obj;
            }
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
        return null;
    }

    private static Object stringToBasic(String value, Class<?> targetType) throws Exception {
        return BasicDeserializer.deserialize(value, targetType);
    }


    private static Object deserialize(String value, Class<?> clazz) throws Exception {
        if (!converters.containsKey(clazz)) {
            return null;
        }
        return converters.get(clazz).apply(value);
    }


    private static <T> T convert(String value, Class<T> clazz) throws Exception {
        if (!converters.containsKey(clazz)) {
            return null;
        }
        return clazz.cast(converters.get(clazz).apply(value));
    }

    private static void initConverters() {
        converters.put(int.class, Integer::parseInt);
        converters.put(Integer.class, Integer::parseInt);
        converters.put(long.class, Long::parseLong);
        converters.put(Long.class, Long::parseLong);
        converters.put(float.class, Float::parseFloat);
        converters.put(Float.class, Float::parseFloat);
        converters.put(double.class, Double::parseDouble);
        converters.put(Double.class, Double::parseDouble);
        converters.put(boolean.class, Boolean::parseBoolean);
        converters.put(Boolean.class, Boolean::parseBoolean);
        converters.put(byte.class, Byte::parseByte);
        converters.put(Byte.class, Byte::parseByte);
        converters.put(short.class, Short::parseShort);
        converters.put(Short.class, Short::parseShort);
        converters.put(java.sql.Date.class, java.sql.Date::valueOf);
        converters.put(Date.class, value -> {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
                return dateFormat.parse(value);
            } catch (Exception e) {
                return null;
            }
        });
        converters.put(Instant.class, Instant::parse);
        converters.put(LocalDate.class, value -> {
            LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
            return localDate;
        });
        converters.put(LocalDateTime.class, value -> {
            LocalDateTime datetime = LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            return datetime;
        });
        converters.put(ZonedDateTime.class, value -> {
            ZonedDateTime datetime = ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            return datetime;
        });
        converters.put(Timestamp.class, value -> {
            LocalDateTime datetime = LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            return Timestamp.valueOf(datetime);
        });
        converters.put(char.class, value -> {
            if (!value.isEmpty()) {
                return value.charAt(0);
            }
            return null;
        });
        converters.put(Character.class, value -> {
            if (!value.isEmpty()) {
                return value.charAt(0);
            }
            return null;
        });
        converters.put(String.class, value -> value);
    }


}
