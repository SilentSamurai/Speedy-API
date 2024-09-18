package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.enums.ValueType;
import org.jooq.impl.SQLDataType;


import java.time.*;
import java.util.*;


public class ValueTypeUtil {

    private static final Map<Class<?>, ValueType> CLASS_TO_VALUE_TYPE_MAP = new HashMap<>();

    static {
        CLASS_TO_VALUE_TYPE_MAP.put(String.class, ValueType.TEXT);
        CLASS_TO_VALUE_TYPE_MAP.put(Boolean.class, ValueType.BOOL);
        CLASS_TO_VALUE_TYPE_MAP.put(boolean.class, ValueType.BOOL);
        CLASS_TO_VALUE_TYPE_MAP.put(Integer.class, ValueType.INT);
        CLASS_TO_VALUE_TYPE_MAP.put(int.class, ValueType.INT);
        CLASS_TO_VALUE_TYPE_MAP.put(Float.class, ValueType.FLOAT);
        CLASS_TO_VALUE_TYPE_MAP.put(float.class, ValueType.FLOAT);
        CLASS_TO_VALUE_TYPE_MAP.put(Double.class, ValueType.FLOAT);
        CLASS_TO_VALUE_TYPE_MAP.put(double.class, ValueType.FLOAT);
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

    public static Class<?> toClass(ValueType valueType) {
        switch (valueType) {
            case BOOL:
                return Boolean.class;
            case TEXT:
                return String.class;
            case INT:
                return Long.class;
            case FLOAT:
                return Double.class;
            case DATE:
                return LocalDate.class;
            case TIME:
                return LocalTime.class;
            case DATE_TIME:
                return LocalDateTime.class;
            case ZONED_DATE_TIME:
                return ZonedDateTime.class;
            case OBJECT:
            case COLLECTION:
            case NULL:
            default:
                throw new IllegalArgumentException("Unsupported value type: " + valueType);
        }
    }

}
