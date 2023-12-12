package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import lombok.Getter;

@Getter
public class SpeedyBoolean implements SpeedyValue {

    private Boolean value;

    public SpeedyBoolean(Boolean value) {
        this.value = value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.BOOL;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }
}
