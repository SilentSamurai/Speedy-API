package com.github.silent.samurai.speedy.enums;

public enum ColumnType {
    INTEGER,
    SMALLINT,
    BIGINT,
    DECIMAL,
    NUMERIC,
    FLOAT,
    REAL,
    DOUBLE,
    CHAR,
    VARCHAR,
    TEXT,
    DATE,
    TIME,
    TIMESTAMP,
    TIMESTAMP_WITH_ZONE,
    BOOLEAN,
    BLOB,
    CLOB,
    UUID;

    public static ColumnType valueOrDefault(String value, ColumnType defVal) {
        if (value == null || value.isBlank()) {
            return defVal;
        }
        return switch (value.toUpperCase()) {
            case "INTEGER" -> INTEGER;
            case "SMALLINT" -> SMALLINT;
            case "BIGINT" -> BIGINT;
            case "DECIMAL" -> DECIMAL;
            case "NUMERIC" -> NUMERIC;
            case "FLOAT" -> FLOAT;
            case "REAL" -> REAL;
            case "DOUBLE" -> DOUBLE;
            case "CHAR" -> CHAR;
            case "VARCHAR" -> VARCHAR;
            case "TEXT" -> TEXT;
            case "DATE" -> DATE;
            case "TIME" -> TIME;
            case "TIMESTAMP" -> TIMESTAMP;
            case "TIMESTAMP_WITH_ZONE" -> TIMESTAMP_WITH_ZONE;
            case "BOOLEAN" -> BOOLEAN;
            case "BLOB" -> BLOB;
            case "CLOB" -> CLOB;
            case "UUID" -> UUID;
            default -> defVal;
        };
    }

    public static ValueType toValueType(ColumnType columnType) {
        return switch (columnType) {
            case INTEGER:
            case SMALLINT:
            case BIGINT:
                yield ValueType.INT;
            case DECIMAL:
            case NUMERIC:
            case FLOAT:
            case REAL:
            case DOUBLE:
                yield ValueType.FLOAT;
            case CHAR:
            case VARCHAR:
            case TEXT:
            case UUID:
                yield ValueType.TEXT;
            case DATE:
                yield ValueType.DATE;
            case TIME:
                yield ValueType.TIME;
            case TIMESTAMP:
                yield ValueType.DATE_TIME;
            case BOOLEAN:
                yield ValueType.BOOL;
            case BLOB:
            case CLOB:
                yield ValueType.OBJECT;
            case TIMESTAMP_WITH_ZONE:
                yield ValueType.ZONED_DATE_TIME; // Mapping for the new type
        };
    }

    public ValueType getValueType() {
        return toValueType(this);
    }

}

