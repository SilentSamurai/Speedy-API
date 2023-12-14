package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;

import java.time.LocalTime;
@Getter
public class SpeedyTime implements SpeedyValue {

    private LocalTime value = null;

    public SpeedyTime(LocalTime value) {
        this.value = value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.TIME;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public boolean isTime() {
        return true;
    }

    @Override
    public LocalTime asTime() {
        return getValue();
    }
}
