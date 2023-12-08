package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;

import java.util.Map;

public class SpeedyEntityKey {
    private final EntityMetadata entityMetadata;
    private Map<KeyFieldMetadata, SpeedyValue> fields;

    public SpeedyEntityKey(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }
}
