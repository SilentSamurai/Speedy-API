package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JavaType2ColumnType {

    static final Map<Class<?>, ColumnType> CLASS_TO_SQL_VALUE_TYPE_MAP = new HashMap<>();

    static {
        // Strings and UUID
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(UUID.class, ColumnType.UUID);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(String.class, ColumnType.VARCHAR);

        // Booleans
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Boolean.class, ColumnType.BOOLEAN);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(boolean.class, ColumnType.BOOLEAN);

        // Integers and numeric types
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Byte.class, ColumnType.INTEGER);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(byte.class, ColumnType.INTEGER);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Short.class, ColumnType.INTEGER);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(short.class, ColumnType.INTEGER);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Integer.class, ColumnType.INTEGER);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(int.class, ColumnType.INTEGER);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Long.class, ColumnType.INTEGER);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(long.class, ColumnType.INTEGER);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(BigInteger.class, ColumnType.BIGINT);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Float.class, ColumnType.FLOAT);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(float.class, ColumnType.FLOAT);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Double.class, ColumnType.DOUBLE);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(double.class, ColumnType.DOUBLE);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(BigDecimal.class, ColumnType.DECIMAL);

        // Date and time
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(java.sql.Date.class, ColumnType.DATE);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Date.class, ColumnType.DATE);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(LocalDate.class, ColumnType.DATE);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(LocalDateTime.class, ColumnType.TIMESTAMP);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(LocalTime.class, ColumnType.TIME);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Timestamp.class, ColumnType.TIMESTAMP);

        // Zoned/offset date-time
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(OffsetDateTime.class, ColumnType.TIMESTAMP_WITH_ZONE);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(ZonedDateTime.class, ColumnType.TIMESTAMP_WITH_ZONE);
        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Instant.class, ColumnType.TIMESTAMP_WITH_ZONE);

        // Collections
//        CLASS_TO_SQL_VALUE_TYPE_MAP.put(Collection.class, SqlValueType.TEXT);
//        CLASS_TO_SQL_VALUE_TYPE_MAP.put(List.class, SqlValueType.TEXT);
//        CLASS_TO_SQL_VALUE_TYPE_MAP.put(ArrayList.class, SqlValueType.TEXT);
//        CLASS_TO_SQL_VALUE_TYPE_MAP.put(LinkedList.class, SqlValueType.TEXT);
    }

    public static ColumnType fromClass(Class<?> clazz) throws NotFoundException {
        if (CLASS_TO_SQL_VALUE_TYPE_MAP.containsKey(clazz)) {
            return CLASS_TO_SQL_VALUE_TYPE_MAP.get(clazz);
        }
        throw new NotFoundException("Column type of '%s' not found".formatted(clazz.getName()));
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
