package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.models.SpeedyDouble;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers finding #1: {@link MinRule}/{@link MaxRule} previously guarded on {@code isNumber()} (true for
 * FLOAT) but called {@code asLong()}, which {@link SpeedyDouble} does not override — hitting the default
 * {@code SpeedyValue.asLong()} that throws {@code ConversionException}. The fix compares via BigDecimal
 * like {@code DecimalMinRule}. These tests assert correct behaviour for both DECIMAL/FLOAT and integer
 * values, including boundaries, so the integer path is also guarded against regression.
 */
class MinMaxRuleDecimalTest {

    private FieldMetadata field(String name) {
        FieldMetadata fm = Mockito.mock(FieldMetadata.class);
        Mockito.when(fm.getOutputPropertyName()).thenReturn(name);
        return fm;
    }

    private List<String> validateMax(long max, com.github.silent.samurai.speedy.interfaces.SpeedyValue v) {
        List<String> errors = new ArrayList<>();
        new MaxRule(max).validate(field("f"), v, errors);
        return errors;
    }

    private List<String> validateMin(long min, com.github.silent.samurai.speedy.interfaces.SpeedyValue v) {
        List<String> errors = new ArrayList<>();
        new MinRule(min).validate(field("f"), v, errors);
        return errors;
    }

    // ---- DECIMAL / FLOAT (the bug) ----

    @Test
    void maxRule_decimalAboveMax_shouldReject() {
        assertEquals(1, validateMax(10, new SpeedyDouble(10.9)).size(),
                "MaxRule must reject 10.9 against max=10 on a FLOAT value");
    }

    @Test
    void maxRule_decimalAtBoundary_shouldPass() {
        assertTrue(validateMax(10, new SpeedyDouble(10.0)).isEmpty(),
                "MaxRule must accept 10.0 against max=10 (inclusive)");
    }

    @Test
    void minRule_negativeDecimalBelowMin_shouldReject() {
        assertEquals(1, validateMin(-10, new SpeedyDouble(-10.9)).size(),
                "MinRule must reject -10.9 against min=-10 on a FLOAT value");
    }

    @Test
    void minRule_decimalAtBoundary_shouldPass() {
        assertTrue(validateMin(-10, new SpeedyDouble(-10.0)).isEmpty(),
                "MinRule must accept -10.0 against min=-10 (inclusive)");
    }

    // ---- INTEGER (regression guard — must keep working) ----

    @Test
    void minRule_integerBelowMin_shouldReject() {
        assertEquals(1, validateMin(18, new SpeedyInt(15L)).size());
    }

    @Test
    void minRule_integerAtBoundary_shouldPass() {
        assertTrue(validateMin(18, new SpeedyInt(18L)).isEmpty());
    }

    @Test
    void maxRule_integerAboveMax_shouldReject() {
        assertEquals(1, validateMax(60, new SpeedyInt(65L)).size());
    }
}
