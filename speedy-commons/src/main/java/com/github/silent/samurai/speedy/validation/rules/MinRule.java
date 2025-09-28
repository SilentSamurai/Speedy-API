package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * Validates that a numeric value is at least {@code min}.
 */
public class MinRule implements FieldRule {
    private final long min;

    public MinRule(long min) {
        this.min = min;
    }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (val.isNumber() && val.asLong() < min) {
            errors.add(fm.getOutputPropertyName() + " must be >= " + min);
        }
    }
}
