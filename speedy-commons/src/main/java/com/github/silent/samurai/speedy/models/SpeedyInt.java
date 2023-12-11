package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import lombok.Getter;

@Getter
public class SpeedyInt implements SpeedyValue {

    private Integer value;

    public SpeedyInt(Integer value) {
        this.value = value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.INT;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }
}
