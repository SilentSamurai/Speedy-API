package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import java.math.BigDecimal;
import java.util.List;

/**
 * Validates that the numeric value is less than or equal to (or strictly less than)
 * a specified maximum as defined by {@code jakarta.validation.constraints.DecimalMax}.
 */
public class DecimalMaxRule implements FieldRule {
    private final BigDecimal max;
    private final boolean inclusive;

    public DecimalMaxRule(String max, boolean inclusive) {
        this.max = new BigDecimal(max);
        this.inclusive = inclusive;
    }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (!val.isNumber()) return;

        BigDecimal num = val.isDouble() ? BigDecimal.valueOf(val.asDouble()) : BigDecimal.valueOf(val.asLong());
        int cmp = num.compareTo(max);
        boolean valid = inclusive ? cmp <= 0 : cmp < 0;
        if (!valid) {
            String op = inclusive ? "<= " : "< ";
            errors.add(fm.getOutputPropertyName() + " must be " + op + max);
        }
    }
}
