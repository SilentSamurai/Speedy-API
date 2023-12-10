package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.enums.ValueType;

public interface SpeedyValue {

    ValueType getValueType();

    boolean isEmpty();

    default boolean isPresent() {
        return !isEmpty();
    }

}
