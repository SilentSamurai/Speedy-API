package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;

public class SpeedyField {

    private final FieldMetadata fieldMetadata;
    private final Object value;

    public SpeedyField(FieldMetadata fieldMetadata, Object value) {
        this.fieldMetadata = fieldMetadata;
        this.value = value;
    }

    public static SpeedyField fromOne(FieldMetadata fieldMetadata, Object value) {
        return new SpeedyField(fieldMetadata, value);
    }
}
