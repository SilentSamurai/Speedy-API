package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * Validates that the provided numeric value is strictly positive (> 0).
 * <p>
 * Supports {@code jakarta.validation.constraints.Positive}.
 */
public class PositiveRule implements FieldRule {

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (!val.isNumber()) return; // non-numeric handled elsewhere
        double num = val.isDouble() ? val.asDouble() : val.asLong();
        if (num <= 0) {
            errors.add(fm.getOutputPropertyName() + " must be > 0");
        }
    }
}
