package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;

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
}
