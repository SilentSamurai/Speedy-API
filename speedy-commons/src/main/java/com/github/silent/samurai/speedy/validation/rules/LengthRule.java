package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * Validates string length within [min,max].
 */
public class LengthRule implements FieldRule {
    private final int min;
    private final int max;

    public LengthRule(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (!val.isText()) return;
        int len = val.asText().length();
        if (len < min || len > max) {
            errors.add(fm.getOutputPropertyName() + " length must be between " + min + " and " + max);
        }
    }
}
