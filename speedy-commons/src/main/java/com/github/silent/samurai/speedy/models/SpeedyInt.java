package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;
import java.util.Objects;

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
    public Long asInt() {
        return getValue().longValue();
    }

    @Override
    public Long asLong() {
        return getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpeedyInt speedyInt = (SpeedyInt) o;
        return Objects.equals(value, speedyInt.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
