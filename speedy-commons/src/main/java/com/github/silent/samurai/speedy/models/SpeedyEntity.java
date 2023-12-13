package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class SpeedyEntity implements SpeedyValue {

    @Getter
    private final EntityMetadata entityMetadata;
    private final Map<String, SpeedyValue> fields = new HashMap<>();

    public SpeedyEntity(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    @Override
    public String toString() {
        return fields.toString();
    }

    public boolean has(FieldMetadata fieldMetadata) {
        return fields.containsKey(fieldMetadata.getOutputPropertyName());
    }

    public SpeedyValue get(FieldMetadata fieldMetadata) {
        return fields.get(fieldMetadata.getOutputPropertyName());
    }

//    public SpeedyValue get(String column) {
//        return fields.get(column);
//    }

    public void put(FieldMetadata fieldMetadata, SpeedyValue value) {
        fields.put(fieldMetadata.getOutputPropertyName(), value);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.OBJECT;
    }

    @Override
    public boolean isEmpty() {
        return entityMetadata == null;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public SpeedyEntity asObject() {
        return this;
    }
}
