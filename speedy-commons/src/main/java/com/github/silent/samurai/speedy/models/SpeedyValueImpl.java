package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import lombok.Getter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Getter
public class SpeedyValueImpl implements SpeedyValue {

    final ValueType valueType;
    final List<Object> values = new LinkedList<>();

    private SpeedyValueImpl(ValueType valueType) {
        this.valueType = valueType;
    }

    private SpeedyValueImpl(ValueType valueType, Object value) {
        this.valueType = valueType;
        this.values.add(value);
    }

    public static SpeedyValueImpl fromOne(ValueType valueType, Object value) {
        return new SpeedyValueImpl(valueType, value);
    }

    public static SpeedyValueImpl fromMany(ValueType valueType, Collection<Object> values) {
        SpeedyValueImpl speedyValue = new SpeedyValueImpl(valueType);
        speedyValue.values.addAll(values);
        return speedyValue;
    }

    @Override
    public Object getSingleValue() {
        if (values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }
}
