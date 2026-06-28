package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.math.BigDecimal;
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
        if (!val.isNumber()) return;
        // Compare via BigDecimal so FLOAT/DECIMAL values are not truncated by (or thrown on) asLong().
        BigDecimal num = val.isDouble() ? BigDecimal.valueOf(val.asDouble()) : BigDecimal.valueOf(val.asLong());
        if (num.compareTo(BigDecimal.valueOf(min)) < 0) {
            errors.add(fm.getOutputPropertyName() + " must be >= " + min);
        }
    }
}
