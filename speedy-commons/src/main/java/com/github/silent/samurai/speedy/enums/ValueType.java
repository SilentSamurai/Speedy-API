package com.github.silent.samurai.speedy.enums;

import java.sql.Date;
import java.time.*;
import java.util.Collection;

public enum ValueType {
    TEXT,
    INT,
    FLOAT,
    DATE,
    TIME,
    DATE_TIME,
    OBJECT,
    COLLECTION;

    public static ValueType fromClass(Class<?> clazz) {
        if (String.class.isAssignableFrom(clazz)) {
            return TEXT;
        } else if (Integer.class.isAssignableFrom(clazz) || int.class.isAssignableFrom(clazz)) {
            return INT;
        } else if (Float.class.isAssignableFrom(clazz) || float.class.isAssignableFrom(clazz)
                || Double.class.isAssignableFrom(clazz) || double.class.isAssignableFrom(clazz)) {
            return FLOAT;
        } else if (Date.class.isAssignableFrom(clazz)) {
            return DATE;
        } else if (LocalTime.class.isAssignableFrom(clazz) || OffsetTime.class.isAssignableFrom(clazz)
                || LocalDateTime.class.isAssignableFrom(clazz) || ZonedDateTime.class.isAssignableFrom(clazz)
                || Instant.class.isAssignableFrom(clazz)) {
            return DATE_TIME;
        } else if (Collection.class.isAssignableFrom(clazz)) {
            return COLLECTION;
        } else {
            return OBJECT;
        }
    }
}
