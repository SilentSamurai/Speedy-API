package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;
import java.util.Objects;

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
    public LocalDateTime asDateTime() {
        return getValue();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpeedyDateTime{");
        sb.append("value=").append(value);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpeedyDateTime that = (SpeedyDateTime) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
