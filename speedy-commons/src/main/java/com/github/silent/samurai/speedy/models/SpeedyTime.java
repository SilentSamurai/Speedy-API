package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;

import java.time.LocalTime;

public class SpeedyTime implements SpeedyValue {

    private LocalTime value;

    public SpeedyTime(LocalTime value) {
        this.value = value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.TIME;
    }
}
