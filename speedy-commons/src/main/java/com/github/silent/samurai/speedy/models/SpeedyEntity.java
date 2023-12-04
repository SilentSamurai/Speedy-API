package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;

import java.util.Map;

public class SpeedyEntity {

    private final EntityMetadata entityMetadata;
    private Map<FieldMetadata, SpeedyField> data;

    public SpeedyEntity(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public SpeedyField get(FieldMetadata fieldMetadata) {
        return data.get(fieldMetadata);
    }

    public void put(FieldMetadata fieldMetadata, SpeedyField value) {
        data.put(fieldMetadata, value);
    }
}
