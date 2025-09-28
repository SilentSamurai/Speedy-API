package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;
import java.util.Objects;

@Getter
public class SpeedyBoolean implements SpeedyValue {

    private Boolean value;

    public SpeedyBoolean(Boolean value) {
        this.value = value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.BOOL;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public Boolean asBoolean() {
        return getValue();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpeedyBoolean{");
        sb.append("value=").append(value);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpeedyBoolean that = (SpeedyBoolean) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
