package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

public class SpeedyNull implements SpeedyValue {

    public static SpeedyNull SPEEDY_NULL = new SpeedyNull();

    @Override
    public ValueType getValueType() {
        return ValueType.NULL;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpeedyNull{");
        sb.append('}');
        return sb.toString();
    }
}
