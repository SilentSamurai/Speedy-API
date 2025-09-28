package com.github.silent.samurai.speedy.enums;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public enum ValueType {
    BOOL,
    TEXT,
    INT,
    FLOAT,
    DATE,
    TIME,
    DATE_TIME,
    ZONED_DATE_TIME,
    OBJECT,
    COLLECTION,
    ENUM,
    ENUM_ORD,
    NULL;

    public Class<?> javaTypeClass() {
        return switch (this) {
            case BOOL -> Boolean.class;
            case TEXT, ENUM -> String.class;
            case INT, ENUM_ORD -> Long.class;
            case FLOAT -> Double.class;
            case DATE -> LocalDate.class;
            case TIME -> LocalTime.class;
            case DATE_TIME -> LocalDateTime.class;
            case ZONED_DATE_TIME -> ZonedDateTime.class;
            case OBJECT, COLLECTION, NULL -> throw new IllegalArgumentException("Unsupported value type: " + this);
        };
    }

}
