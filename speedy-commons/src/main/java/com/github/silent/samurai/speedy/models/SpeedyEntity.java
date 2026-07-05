package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SpeedyEntity implements SpeedyValue {
    private final EntityMetadata entityMetadata;
    private final Map<String, SpeedyValue> fields = new HashMap<>();

    public SpeedyEntity(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    public boolean has(FieldMetadata fieldMetadata) {
        return fields.containsKey(fieldMetadata.getOutputPropertyName());
    }

    public SpeedyValue getOrDefault(FieldMetadata fieldMetadata, SpeedyValue speedyValue) {
        if (!has(fieldMetadata)) {
            return speedyValue;
        }
        return fields.get(fieldMetadata.getOutputPropertyName());
    }

    public SpeedyValue getOrDefault(String fieldName, SpeedyValue speedyValue) throws NotFoundException {
        FieldMetadata fieldMetadata = entityMetadata.field(fieldName);
        if (!has(fieldMetadata)) {
            return speedyValue;
        }
        return fields.get(fieldMetadata.getOutputPropertyName());
    }

    public SpeedyValue get(String fieldName) throws NotFoundException {
        FieldMetadata fieldMetadata = entityMetadata.field(fieldName);
        return get(fieldMetadata);
    }

    public SpeedyValue get(FieldMetadata fieldMetadata) {
        if (!has(fieldMetadata)) {
            throw new IllegalArgumentException("Field '%s' does not exist".formatted(fieldMetadata.getOutputPropertyName()));
        }
        return fields.get(fieldMetadata.getOutputPropertyName());
    }

    public void put(String fieldName, SpeedyValue value) throws NotFoundException {
        FieldMetadata fieldMetadata = entityMetadata.field(fieldName);
        fields.put(fieldMetadata.getOutputPropertyName(), value);
    }

    public void put(FieldMetadata fieldMetadata, SpeedyValue value) {
        fields.put(fieldMetadata.getOutputPropertyName(), value);
    }

    public void remove(FieldMetadata fieldMetadata) {
        fields.remove(fieldMetadata.getOutputPropertyName());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpeedyEntity that = (SpeedyEntity) o;
        if (!Objects.equals(entityMetadata, that.entityMetadata)) {
            return false;
        }
        for (KeyFieldMetadata kf : entityMetadata.getKeyFields()) {
            if (!Objects.equals(
                    this.fields.get(kf.getOutputPropertyName()),
                    that.fields.get(kf.getOutputPropertyName()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(entityMetadata);
        for (KeyFieldMetadata kf : entityMetadata.getKeyFields()) {
            SpeedyValue v = fields.get(kf.getOutputPropertyName());
            result = 31 * result + (v != null ? v.hashCode() : 0);
        }
        return result;
    }
}
