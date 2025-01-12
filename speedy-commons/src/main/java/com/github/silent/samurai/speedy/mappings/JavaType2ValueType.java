package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;

public class JavaType2ValueType {

    static final Map<Class<?>, ValueType> CLASS_TO_VALUE_TYPE_MAP = new HashMap<>();

    static {
        CLASS_TO_VALUE_TYPE_MAP.put(UUID.class, ValueType.TEXT);
        CLASS_TO_VALUE_TYPE_MAP.put(String.class, ValueType.TEXT);
        CLASS_TO_VALUE_TYPE_MAP.put(Boolean.class, ValueType.BOOL);
        CLASS_TO_VALUE_TYPE_MAP.put(boolean.class, ValueType.BOOL);
        CLASS_TO_VALUE_TYPE_MAP.put(Integer.class, ValueType.INT);
        CLASS_TO_VALUE_TYPE_MAP.put(int.class, ValueType.INT);
        CLASS_TO_VALUE_TYPE_MAP.put(Long.class, ValueType.INT);
        CLASS_TO_VALUE_TYPE_MAP.put(long.class, ValueType.INT);
        CLASS_TO_VALUE_TYPE_MAP.put(Short.class, ValueType.INT);
        CLASS_TO_VALUE_TYPE_MAP.put(short.class, ValueType.INT);
        CLASS_TO_VALUE_TYPE_MAP.put(BigInteger.class, ValueType.INT);
//        CLASS_TO_VALUE_TYPE_MAP.put(Number.class, ValueType.INT);
        CLASS_TO_VALUE_TYPE_MAP.put(Float.class, ValueType.FLOAT);
        CLASS_TO_VALUE_TYPE_MAP.put(float.class, ValueType.FLOAT);
        CLASS_TO_VALUE_TYPE_MAP.put(Double.class, ValueType.FLOAT);
        CLASS_TO_VALUE_TYPE_MAP.put(double.class, ValueType.FLOAT);
        CLASS_TO_VALUE_TYPE_MAP.put(BigDecimal.class, ValueType.FLOAT);
        //
        CLASS_TO_VALUE_TYPE_MAP.put(java.sql.Date.class, ValueType.DATE);
        CLASS_TO_VALUE_TYPE_MAP.put(Date.class, ValueType.DATE);
        CLASS_TO_VALUE_TYPE_MAP.put(LocalDate.class, ValueType.DATE);
        CLASS_TO_VALUE_TYPE_MAP.put(OffsetDateTime.class, ValueType.ZONED_DATE_TIME);
        CLASS_TO_VALUE_TYPE_MAP.put(ZonedDateTime.class, ValueType.ZONED_DATE_TIME);
        CLASS_TO_VALUE_TYPE_MAP.put(Instant.class, ValueType.ZONED_DATE_TIME);
        CLASS_TO_VALUE_TYPE_MAP.put(LocalDateTime.class, ValueType.DATE_TIME);
        CLASS_TO_VALUE_TYPE_MAP.put(LocalTime.class, ValueType.TIME);
        CLASS_TO_VALUE_TYPE_MAP.put(Collection.class, ValueType.COLLECTION);
        CLASS_TO_VALUE_TYPE_MAP.put(List.class, ValueType.COLLECTION);
        CLASS_TO_VALUE_TYPE_MAP.put(ArrayList.class, ValueType.COLLECTION);
        CLASS_TO_VALUE_TYPE_MAP.put(LinkedList.class, ValueType.COLLECTION);
    }

    public static ValueType fromClass(Class<?> clazz) {
        return CLASS_TO_VALUE_TYPE_MAP.getOrDefault(clazz, ValueType.OBJECT);
    }

    public static Class<?> toBasicStandardJavaType(ValueType valueType) {
        return switch (valueType) {
            case BOOL -> Boolean.class;
            case TEXT -> String.class;
            case INT -> Long.class;
            case FLOAT -> Double.class;
            case DATE -> LocalDate.class;
            case TIME -> LocalTime.class;
            case DATE_TIME -> LocalDateTime.class;
            case ZONED_DATE_TIME -> ZonedDateTime.class;
            default -> throw new IllegalArgumentException("Unsupported value type: " + valueType);
        };
    }


}
