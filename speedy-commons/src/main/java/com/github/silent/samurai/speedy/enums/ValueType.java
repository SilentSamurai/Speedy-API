package com.github.silent.samurai.speedy.enums;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;

import java.sql.Date;
import java.sql.Time;
import java.time.*;
import java.util.Collection;

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
    NULL;

}
