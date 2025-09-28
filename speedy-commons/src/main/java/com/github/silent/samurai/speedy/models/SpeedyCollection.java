package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;
import java.util.Objects;

import java.util.Collection;

@Getter
public class SpeedyCollection implements SpeedyValue {

    private Collection<SpeedyValue> value;

    public SpeedyCollection(Collection<SpeedyValue> collection) {
        this.value = collection;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.COLLECTION;
    }

    @Override
    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    @Override
    public Collection<SpeedyValue> asCollection() {
        return getValue();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpeedyCollection{");
        sb.append("value=").append(value);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpeedyCollection that = (SpeedyCollection) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
