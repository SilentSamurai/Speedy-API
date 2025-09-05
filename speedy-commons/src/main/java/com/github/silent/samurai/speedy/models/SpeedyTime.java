package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;
import java.util.Objects;

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
    public LocalTime asTime() {
        return getValue();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpeedyTime{");
        sb.append("value=").append(value);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpeedyTime that = (SpeedyTime) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
