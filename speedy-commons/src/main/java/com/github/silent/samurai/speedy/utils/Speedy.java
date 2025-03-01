package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public class Speedy {

    public SpeedyNull NULL = SpeedyNull.SPEEDY_NULL;
    public SpeedyInt I_ZERO = from(0L);
    public SpeedyDouble F_ZERO = from(0.0);

    public static SpeedyBoolean from(Boolean value) {
        return new SpeedyBoolean(value);
    }

    public static SpeedyText from(String value) {
        return new SpeedyText(value);
    }

    public static SpeedyInt from(Long value) {
        return new SpeedyInt(value);
    }

    public static SpeedyDouble from(Double value) {
        return new SpeedyDouble(value);
    }

    public static SpeedyDate from(LocalDate value) {
        return new SpeedyDate(value);
    }

    public static SpeedyTime from(LocalTime value) {
        return new SpeedyTime(value);
    }

    public static SpeedyDateTime from(LocalDateTime value) {
        return new SpeedyDateTime(value);
    }

    public static SpeedyZonedDateTime from(ZonedDateTime zonedDateTime) {
        return new SpeedyZonedDateTime(zonedDateTime);
    }

    public static SpeedyNull fromNull() {
        return SpeedyNull.SPEEDY_NULL;
    }
}
