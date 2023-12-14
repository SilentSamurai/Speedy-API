package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SpeedyDateTime implements SpeedyValue {

    private LocalDateTime value;

    public SpeedyDateTime(LocalDateTime value) {
        this.value = value;
    }


    @Override
    public ValueType getValueType() {
        return ValueType.DATE_TIME;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public boolean isDateTime() {
        return true;
    }

    @Override
    public LocalDateTime asDateTime() {
        return getValue();
    }
}
