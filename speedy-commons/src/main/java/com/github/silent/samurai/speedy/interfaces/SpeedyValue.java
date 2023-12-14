package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;

public interface SpeedyValue {

    ValueType getValueType();

    boolean isEmpty();

    default boolean isPresent() {
        return !isEmpty();
    }

    default boolean isText() {
        return false;
    }

    default String asText() {
        throw new ConversionException("Cannot convert to text");
    }

    default boolean isInt() {
        return false;
    }

    default Integer asInt() {
        throw new ConversionException("Cannot convert to integer");
    }

    default boolean isObject() {
        return false;
    }

    default SpeedyEntity asObject() {
        throw new ConversionException("Cannot convert to object");
    }

    default boolean isCollection() {
        return false;
    }

    default Collection<SpeedyValue> asCollection() {
        throw new ConversionException("Cannot convert to integer");
    }

    default boolean isDouble() {
        return false;
    }

    default Double asDouble() {
        throw new ConversionException("Cannot convert to double");
    }

    default boolean isBoolean() {
        return false;
    }

    default Boolean asBoolean() {
        throw new ConversionException("Cannot convert to boolean");
    }

    default boolean isDate() {
        return false;
    }

    default LocalDate asDate() {
        throw new ConversionException("Cannot convert to date");
    }

    default boolean isDateTime() {
        return false;
    }

    default LocalDateTime asDateTime() {
        throw new ConversionException("Cannot convert to datetime");
    }

    default boolean isTime() {
        return false;
    }

    default LocalTime asTime() {
        throw new ConversionException("Cannot convert to time");
    }
}
