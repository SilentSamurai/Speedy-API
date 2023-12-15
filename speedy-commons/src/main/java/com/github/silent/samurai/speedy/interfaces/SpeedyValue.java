package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collection;

public interface SpeedyValue {

    ValueType getValueType();

    boolean isEmpty();

    default boolean isPresent() {
        return !isEmpty();
    }

    default boolean isNull() {
        return getValueType() == ValueType.NULL;
    }

    default boolean isText() {
        return getValueType() == ValueType.TEXT;
    }

    default String asText() {
        throw new ConversionException("Cannot convert to text");
    }

    default boolean isInt() {
        return getValueType() == ValueType.INT;
    }

    default Integer asInt() {
        throw new ConversionException("Cannot convert to integer");
    }

    default boolean isLong() {
        return getValueType() == ValueType.INT;
    }

    default Long asLong() {
        throw new ConversionException("Cannot convert to long");
    }

    default boolean isObject() {
        return getValueType() == ValueType.OBJECT;
    }

    default SpeedyEntity asObject() {
        throw new ConversionException("Cannot convert to object");
    }

    default boolean isCollection() {
        return getValueType() == ValueType.COLLECTION;
    }

    default Collection<SpeedyValue> asCollection() {
        throw new ConversionException("Cannot convert to integer");
    }

    default boolean isDouble() {
        return getValueType() == ValueType.FLOAT;
    }

    default Double asDouble() {
        throw new ConversionException("Cannot convert to double");
    }

    default boolean isBoolean() {
        return getValueType() == ValueType.BOOL;
    }

    default Boolean asBoolean() {
        throw new ConversionException("Cannot convert to boolean");
    }

    default boolean isDate() {
        return getValueType() == ValueType.DATE;
    }

    default LocalDate asDate() {
        throw new ConversionException("Cannot convert to date");
    }

    default boolean isDateTime() {
        return getValueType() == ValueType.DATE_TIME;
    }

    default LocalDateTime asDateTime() {
        throw new ConversionException("Cannot convert to datetime");
    }

    default boolean isTime() {
        return getValueType() == ValueType.TIME;
    }

    default LocalTime asTime() {
        throw new ConversionException("Cannot convert to time");
    }

    default boolean isZonedDateTime() {
        return getValueType() == ValueType.ZONED_DATE_TIME;
    }

    default ZonedDateTime asZonedDateTime() {
        throw new ConversionException("Cannot convert to zoneddatetime");
    }

    default boolean isValue() {
        return getValueType() == ValueType.NULL ||
                getValueType() == ValueType.TEXT ||
                getValueType() == ValueType.INT ||
                getValueType() == ValueType.FLOAT ||
                getValueType() == ValueType.DATE ||
                getValueType() == ValueType.DATE_TIME ||
                getValueType() == ValueType.TIME ||
                getValueType() == ValueType.ZONED_DATE_TIME ||
                getValueType() == ValueType.BOOL;
    }

    default boolean isNumber() {
        return getValueType() == ValueType.INT ||
                getValueType() == ValueType.FLOAT;
    }

    default boolean isTemporal() {
        return getValueType() == ValueType.DATE ||
                getValueType() == ValueType.DATE_TIME ||
                getValueType() == ValueType.TIME ||
                getValueType() == ValueType.ZONED_DATE_TIME;
    }

    default boolean isContainer() {
        return getValueType() == ValueType.OBJECT ||
                getValueType() == ValueType.COLLECTION;
    }

}
