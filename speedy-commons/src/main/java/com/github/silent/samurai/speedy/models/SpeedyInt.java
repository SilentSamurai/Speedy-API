package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;

@Getter
public class SpeedyInt implements SpeedyValue {

    private Long value;

    public SpeedyInt(Long value) {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpeedyInt{");
        sb.append("value=").append(value);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public Integer asInt() {
        return getValue().intValue();
    }

    @Override
    public Long asLong() {
        return getValue();
    }
}
