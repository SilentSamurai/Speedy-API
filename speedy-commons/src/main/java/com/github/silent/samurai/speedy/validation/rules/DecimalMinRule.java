package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validates that the numeric value is greater than or equal to (or strictly greater than)
 * a specified minimum as defined by {@code jakarta.validation.constraints.DecimalMin}.
 */
public class DecimalMinRule implements FieldRule {

    private final BigDecimal min;
    private final boolean inclusive;

    public DecimalMinRule(String min, boolean inclusive) {
        this.min = new BigDecimal(min);
        this.inclusive = inclusive;
    }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (!val.isNumber()) return;

        BigDecimal num = val.isDouble() ? BigDecimal.valueOf(val.asDouble()) : BigDecimal.valueOf(val.asLong());
        int cmp = num.compareTo(min);
        boolean valid = inclusive ? cmp >= 0 : cmp > 0;
        if (!valid) {
            String op = inclusive ? ">= " : "> ";
            errors.add(fm.getOutputPropertyName() + " must be " + op + min);
        }
    }
}
