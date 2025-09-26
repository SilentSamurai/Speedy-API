package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * Ensures numeric value does not exceed {@code max}.
 */
public class MaxRule implements FieldRule {
    private final long max;

    public MaxRule(long max) {
        this.max = max;
    }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (val.isNumber() && val.asLong() > max) {
            errors.add(fm.getOutputPropertyName() + " must be <= " + max);
        }
    }
}
