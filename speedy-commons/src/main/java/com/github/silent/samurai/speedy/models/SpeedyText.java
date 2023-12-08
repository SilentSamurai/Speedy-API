package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;

public class SpeedyText implements SpeedyValue {

    private String value;

    public SpeedyText(String value) {
        this.value = value;
    }


    @Override
    public ValueType getValueType() {
        return ValueType.TEXT;
    }
}
