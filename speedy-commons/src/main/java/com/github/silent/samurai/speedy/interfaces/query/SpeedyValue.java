package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.models.SpeedyEntity;

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
}
