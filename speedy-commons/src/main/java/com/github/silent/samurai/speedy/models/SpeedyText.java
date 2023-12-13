package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import lombok.Getter;

@Getter
public class SpeedyText implements SpeedyValue {

    private String value;

    public SpeedyText(String value) {
        this.value = value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.TEXT;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public boolean isText() {
        return true;
    }

    @Override
    public String asText() {
        return getValue();
    }
}
