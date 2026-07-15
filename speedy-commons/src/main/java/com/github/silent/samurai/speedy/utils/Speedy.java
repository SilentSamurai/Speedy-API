package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public class Speedy {

    public static final SpeedyNull NULL = SpeedyNull.SPEEDY_NULL;
    public static final SpeedyInt I_ZERO = from(0L);
    public static final SpeedyDouble F_ZERO = from(0.0);

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

    /// Converts a plain Java value of unknown static type into a {@link SpeedyValue}, dispatching
    /// on its runtime type. For callers holding a value with no {@code FieldMetadata} to key off
    /// of — policy variables, for instance — where {@code JavaTypeRegistry} cannot be used because
    /// it needs a target {@link com.github.silent.samurai.speedy.enums.ValueType} to pick between
    /// variant codecs.
    ///
    /// <p>This is also the authority on which Java types are accepted as such a value: anything
    /// outside the supported set is rejected outright rather than silently coerced. {@code
    /// BigDecimal}/{@code BigInteger} are deliberately unsupported — without field metadata there
    /// is no way to tell whether they mean {@code FLOAT} or {@code INT}, and guessing would be
    /// worse than failing. An existing {@link SpeedyValue} passes through unchanged.</p>
    ///
    /// @throws ConversionException if {@code value} is not a supported Java type
    public static SpeedyValue from(Object value) {
        if (value == null) return SpeedyNull.SPEEDY_NULL;
        if (value instanceof SpeedyValue speedyValue) return speedyValue;
        if (value instanceof String string) return from(string);
        if (value instanceof Boolean bool) return from(bool);
        if (value instanceof Long longValue) return from(longValue);
        if (value instanceof Integer intValue) return from((long) intValue);
        if (value instanceof Double doubleValue) return from(doubleValue);
        if (value instanceof Float floatValue) return from((double) floatValue);
        if (value instanceof LocalDate date) return from(date);
        if (value instanceof LocalTime time) return from(time);
        if (value instanceof LocalDateTime dateTime) return from(dateTime);
        if (value instanceof ZonedDateTime zonedDateTime) return from(zonedDateTime);
        throw new ConversionException(
                "Unsupported value type " + value.getClass().getName()
                        + "; allowed types: String, Boolean, Long, Integer, Double, Float, "
                        + "LocalDate, LocalTime, LocalDateTime, ZonedDateTime (or a SpeedyValue)");
    }

    public static SpeedyNull fromNull() {
        return SpeedyNull.SPEEDY_NULL;
    }
}
