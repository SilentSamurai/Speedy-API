package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;

import java.util.Collection;

public class SpeedyCollection implements SpeedyValue {

    private Collection<SpeedyValue> collection;

    public SpeedyCollection(Collection<SpeedyValue> collection) {
        this.collection = collection;
    }

    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public ValueType getValueType() {
        return ValueType.COLLECTION;
    }
}
