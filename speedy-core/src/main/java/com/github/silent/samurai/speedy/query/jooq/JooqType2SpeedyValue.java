package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.ThrowingBiFunction;
import com.github.silent.samurai.speedy.models.*;
import org.jooq.DataType;
import org.jooq.impl.SQLDataType;

import java.util.HashMap;
import java.util.Map;

public class JooqType2SpeedyValue {
    private static final Map<String, ThrowingBiFunction<Object, ColumnType, SpeedyValue, SpeedyHttpException>> converters = new HashMap<>();

    public static <T> ThrowingBiFunction<Object, ColumnType, SpeedyValue, SpeedyHttpException> get(ColumnType columnType, DataType<T> dataType) {
        String key = dataType.getName() + columnType.name();
        return converters.get(key);
    }

    public static boolean has(ColumnType columnType, DataType<?> dataType) {
        String key = dataType.getName() + columnType.name();
        return converters.containsKey(key);
    }

    public static <T> void put(ColumnType columnType, DataType<T> dataType,
                               ThrowingBiFunction<T, ColumnType, SpeedyValue, SpeedyHttpException> lambda) {
        String key = dataType.getName() + columnType.name();
        converters.put(key, (ThrowingBiFunction<Object, ColumnType, SpeedyValue, SpeedyHttpException>) lambda);
    }

    static {
        initConverters();
    }

    public static <T> SpeedyValue convert(DataType<T> dataType, ColumnType columnType, Object instance) throws SpeedyHttpException {
        Class<T> clazz = dataType.getType();
        if (instance == null || !has(columnType, dataType)) {
            return SpeedyNull.SPEEDY_NULL;
        }
        var converter = get(columnType, dataType);
        return converter.apply(instance, columnType);
    }

    private static void initConverters() {
        put(ColumnType.UUID, SQLDataType.UUID, (instance, sqlValueType) ->
                new SpeedyText(instance.toString())
        );
        put(ColumnType.INTEGER, SQLDataType.INTEGER, (instance, valueType) ->
                new SpeedyInt(instance.longValue())
        );
        put(ColumnType.SMALLINT, SQLDataType.SMALLINT, (instance, valueType) ->
                new SpeedyInt(instance.longValue())
        );
        put(ColumnType.BIGINT, SQLDataType.BIGINT, (instance, valueType) ->
                new SpeedyInt(instance)
        );
        put(ColumnType.DECIMAL, SQLDataType.DECIMAL, (instance, valueType) ->
                // TODO: add support for big decimal
                new SpeedyDouble(instance.doubleValue())
        );
        put(ColumnType.NUMERIC, SQLDataType.NUMERIC, (instance, valueType) ->
                new SpeedyDouble(instance.doubleValue())
        );
        put(ColumnType.FLOAT, SQLDataType.FLOAT, (instance, valueType) ->
                new SpeedyDouble(instance)
        );
        put(ColumnType.REAL, SQLDataType.REAL, (instance, valueType) ->
                new SpeedyDouble(instance.doubleValue())
        );
        put(ColumnType.DOUBLE, SQLDataType.DOUBLE, (instance, valueType) ->
                new SpeedyDouble(instance)
        );
        put(ColumnType.CHAR, SQLDataType.CHAR, (instance, valueType) ->
                new SpeedyText(instance)
        );
        put(ColumnType.VARCHAR, SQLDataType.VARCHAR, (instance, valueType) ->
                new SpeedyText(instance)
        );
        put(ColumnType.TEXT, SQLDataType.CLOB, (instance, valueType) ->
                new SpeedyText(instance)
        );
        put(ColumnType.DATE, SQLDataType.DATE, (instance, valueType) ->
                new SpeedyDate(instance.toLocalDate())
        );
        put(ColumnType.TIME, SQLDataType.TIME, (instance, valueType) ->
                new SpeedyTime(instance.toLocalTime())
        );
        put(ColumnType.TIMESTAMP, SQLDataType.TIMESTAMP, (instance, valueType) ->
                new SpeedyDateTime(instance.toLocalDateTime())
        );
        put(ColumnType.TIMESTAMP_WITH_ZONE, SQLDataType.TIMESTAMPWITHTIMEZONE, (instance, valueType) ->
                new SpeedyZonedDateTime(instance.toZonedDateTime())
        );
        put(ColumnType.BOOLEAN, SQLDataType.BOOLEAN, (instance, valueType) ->
                new SpeedyBoolean(instance)
        );
        put(ColumnType.CLOB, SQLDataType.CLOB, (instance, valueType) ->
                new SpeedyText(instance)
        );
//        put(SpeedyDbType.BLOB, SQLDataType.BLOB, (instance, valueType) ->
//                new SpeedyBlob((byte[]) instance)
//        );
    }

}
