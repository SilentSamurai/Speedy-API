package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;

@Getter
public class SpeedyDouble implements SpeedyValue {

    private Double value;

    public SpeedyDouble(Double value) {
        this.value = value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.FLOAT;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public Double asDouble() {
        return getValue();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpeedyDouble{");
        sb.append("value=").append(value);
        sb.append('}');
        return sb.toString();
    }
}
