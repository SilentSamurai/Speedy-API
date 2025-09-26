package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import java.math.BigDecimal;
import java.util.List;

/**
 * Validates the number of integer and fraction digits of a numeric value as defined by
 * {@code jakarta.validation.constraints.Digits}.
 */
public class DigitsRule implements FieldRule {

    private final int integerDigits;
    private final int fractionDigits;

    public DigitsRule(int integerDigits, int fractionDigits) {
        this.integerDigits = integerDigits;
        this.fractionDigits = fractionDigits;
    }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (!val.isNumber()) return;

        BigDecimal num = val.isDouble() ? BigDecimal.valueOf(val.asDouble()) : BigDecimal.valueOf(val.asLong());
        num = num.stripTrailingZeros();
        int intPart = num.precision() - num.scale();
        int fracPart = Math.max(num.scale(), 0);

        if (intPart > integerDigits || fracPart > fractionDigits) {
            errors.add(String.format("%s numeric value out of bounds (<%d digits>.<%d digits> expected)",
                    fm.getOutputPropertyName(), integerDigits, fractionDigits));
        }
    }
}
