package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;

import java.util.Map;

public class SpeedyEntity implements SpeedyValue {

    private final EntityMetadata entityMetadata;
    private Map<FieldMetadata, SpeedyValue> fields;

    public SpeedyEntity(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    @Override
    public String toString() {
        return fields.toString();
    }

    public SpeedyValue get(FieldMetadata fieldMetadata) {
        return fields.get(fieldMetadata);
    }

    public void put(FieldMetadata fieldMetadata, SpeedyValue value) {
        fields.put(fieldMetadata, value);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.OBJECT;
    }
}
