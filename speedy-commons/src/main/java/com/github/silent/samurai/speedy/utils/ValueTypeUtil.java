package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.enums.ValueType;


import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class ValueTypeUtil {

    public static boolean isTimeFormatValid(String value) {
        try {
            LocalTime.parse(value, DateTimeFormatter.ISO_TIME);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    public static boolean isDateFormatValid(String value) {
        try {
            LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    public static boolean isDateTimeFormatValid(String value) {
        try {
            LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    public static boolean isZonedDateTimeValid(String value) {
        try {
            ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

}
