package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.HashMap;
import java.util.Map;

public class SpeedyEntity implements SpeedyValue {
    private final EntityMetadata entityMetadata;
    private final Map<String, SpeedyValue> fields = new HashMap<>();

    public SpeedyEntity(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    public boolean has(FieldMetadata fieldMetadata) {
        return fields.containsKey(fieldMetadata.getOutputPropertyName());
    }

    public SpeedyValue get(FieldMetadata fieldMetadata) {
        if (!has(fieldMetadata)) {
            throw new IllegalArgumentException("Field " + fieldMetadata.getOutputPropertyName() + " does not exist");
        }
        return fields.get(fieldMetadata.getOutputPropertyName());
    }

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
    public SpeedyEntity asObject() {
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpeedyEntity{");
        sb.append("fields=").append(fields);
        sb.append('}');
        return sb.toString();
    }

    public EntityMetadata getMetadata() {
        return entityMetadata;
    }
}
