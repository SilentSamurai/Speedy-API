package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;

import java.util.Map;

public class SpeedyEntityKey {
    private final EntityMetadata entityMetadata;
    private Map<FieldMetadata, SpeedyField> data;

    public SpeedyEntityKey(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }
}
