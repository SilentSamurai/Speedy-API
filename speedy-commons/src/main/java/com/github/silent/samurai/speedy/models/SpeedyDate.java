package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;

import java.time.LocalDate;

public class SpeedyDate implements SpeedyValue {

    private LocalDate value;

    public SpeedyDate(LocalDate value) {
        this.value = value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.DATE;
    }
}
