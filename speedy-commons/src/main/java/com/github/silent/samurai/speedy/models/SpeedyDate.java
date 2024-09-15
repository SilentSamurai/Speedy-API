package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class SpeedyDate implements SpeedyValue {

    private LocalDate value;

    public SpeedyDate(LocalDate value) {
        this.value = value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.DATE;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public LocalDate asDate() {
        return getValue();
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpeedyDate{");
        sb.append("value=").append(value);
        sb.append('}');
        return sb.toString();
    }
}
