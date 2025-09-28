package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import java.util.List;

/**
 * Validates that the numeric value is positive or zero (>= 0).
 * <p>
 * Supports {@code jakarta.validation.constraints.PositiveOrZero}.
 */
public class PositiveOrZeroRule implements FieldRule {
    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (!val.isNumber()) return;
        double num = val.isDouble() ? val.asDouble() : val.asLong();
        if (num < 0) {
            errors.add(fm.getOutputPropertyName() + " must be >= 0");
        }
    }
}
