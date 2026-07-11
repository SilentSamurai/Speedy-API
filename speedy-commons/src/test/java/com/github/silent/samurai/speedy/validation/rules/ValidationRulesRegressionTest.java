package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyTime;
import com.github.silent.samurai.speedy.models.SpeedyZonedDateTime;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRulesRegressionTest {

    private static List<String> runRule(FieldRule rule, SpeedyValue val) {
        List<String> errors = new ArrayList<>();
        rule.validate(new FakeFieldMetadata(), val, errors);
        return errors;
    }

    @Test
    void minRule_accumulatesErrorForEnumOrdInsteadOfThrowing() {
        SpeedyEnumOrdValue enumOrd = new SpeedyEnumOrdValue(1L);
        List<String> errors = runRule(new MinRule(5), enumOrd);
        assertEquals(1, errors.size(), "MinRule should accumulate an error for ENUM_ORD, not throw");
        assertTrue(errors.get(0).contains("must be >= 5"));
    }

    @Test
    void maxRule_accumulatesErrorForEnumOrdInsteadOfThrowing() {
        SpeedyEnumOrdValue enumOrd = new SpeedyEnumOrdValue(10L);
        List<String> errors = runRule(new MaxRule(5), enumOrd);
        assertEquals(1, errors.size());
    }

    @Test
    void positiveRule_accumulatesErrorForEnumOrdZero() {
        SpeedyEnumOrdValue enumOrd = new SpeedyEnumOrdValue(0L);
        List<String> errors = runRule(new PositiveRule(), enumOrd);
        assertEquals(1, errors.size());
    }

    @Test
    void futureRule_rejectsPastZonedDateTime() {
        Clock fixedPast = Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC"));
        FutureRule rule = new FutureRule(null, fixedPast);
        ZonedDateTime past = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        List<String> errors = runRule(rule, new SpeedyZonedDateTime(past));
        assertEquals(1, errors.size(), "Past ZonedDateTime must fail @SpeedyFuture");
    }

    @Test
    void futureRule_acceptsFutureZonedDateTime() {
        Clock fixedPast = Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC"));
        FutureRule rule = new FutureRule(null, fixedPast);
        ZonedDateTime future = ZonedDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        List<String> errors = runRule(rule, new SpeedyZonedDateTime(future));
        assertTrue(errors.isEmpty(), "Future ZonedDateTime must pass @SpeedyFuture");
    }

    @Test
    void pastRule_rejectsFutureZonedDateTime() {
        Clock fixedPast = Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC"));
        PastRule rule = new PastRule(null, fixedPast);
        ZonedDateTime future = ZonedDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        List<String> errors = runRule(rule, new SpeedyZonedDateTime(future));
        assertEquals(1, errors.size(), "Future ZonedDateTime must fail @SpeedyPast");
    }

    @Test
    void futureRule_handlesTime() {
        Clock fixed = Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC"));
        FutureRule rule = new FutureRule(null, fixed);
        LocalTime past = LocalTime.of(5, 0);
        List<String> errors = runRule(rule, new SpeedyTime(past));
        assertEquals(1, errors.size(), "Past LocalTime must fail @SpeedyFuture at fixed 10:00");
    }

    @Test
    void dateRangeRule_rejectsNullBounds() {
        assertThrows(IllegalArgumentException.class, () -> new DateRangeRule(null, "2025-01-01", null));
        assertThrows(IllegalArgumentException.class, () -> new DateRangeRule("2025-01-01", null, null));
    }

    @Test
    void dateRangeRule_rejectsMalformedDate() {
        assertThrows(IllegalArgumentException.class, () -> new DateRangeRule("not-a-date", "2025-01-01", null));
    }

    @Test
    void dateRangeRule_rejectsInvertedRange() {
        assertThrows(IllegalArgumentException.class, () -> new DateRangeRule("2025-12-01", "2025-01-01", null));
    }

    @Test
    void dateRangeRule_acceptsValidRange() {
        assertDoesNotThrow(() -> new DateRangeRule("2025-01-01", "2025-12-31", null));
    }

    static class FakeFieldMetadata extends com.github.silent.samurai.speedy.metadata.FieldMetadataImpl {
        FakeFieldMetadata() {
            super(
                    ColumnType.INTEGER,
                    ValueType.INT,
                    "cost",
                    "cost",
                    false, false, true, true, false, false, false, true, true, false, false,
                    null, null, null, java.util.List.of(),
                    java.util.Optional.empty()
            );
        }
    }

    /// Minimal SpeedyValue that mimics SpeedyEnum's ENUM_ORD behavior:
    /// overrides asInt() but NOT asLong(), so asLong() throws ConversionException.
    static class SpeedyEnumOrdValue implements SpeedyValue {
        private final com.github.silent.samurai.speedy.models.SpeedyInt delegate;

        SpeedyEnumOrdValue(Long ord) {
            this.delegate = new com.github.silent.samurai.speedy.models.SpeedyInt(ord);
        }

        @Override
        public ValueType getValueType() {
            return ValueType.ENUM_ORD;
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public Long asInt() {
            return delegate.asInt();
        }

        @Override
        public Long asEnumOrd() {
            return delegate.asInt();
        }
    }
}
