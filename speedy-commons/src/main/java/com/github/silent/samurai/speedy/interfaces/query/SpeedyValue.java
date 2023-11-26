package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.enums.ValueType;

import java.util.List;

public interface SpeedyValue {

    ValueType getValueType();

    Object getSingleValue();

    List<Object> getValues();

}
