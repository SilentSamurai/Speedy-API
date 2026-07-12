package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import com.github.silent.samurai.speedy.models.SpeedyText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyValueEqualsTest {

    @Test
    void sameType_comparesByValue() {
        assertTrue(PolicyValueEquals.equals(new SpeedyText("u1"), new SpeedyText("u1")));
        assertFalse(PolicyValueEquals.equals(new SpeedyText("u1"), new SpeedyText("u2")));
        assertTrue(PolicyValueEquals.equals(new SpeedyInt(42L), new SpeedyInt(42L)));
    }

    @Test
    void crossType_fallsBackToTextComparison() {
        // A principal id delivered as text vs. a numeric key column should still compare equal.
        assertTrue(PolicyValueEquals.equals(new SpeedyText("42"), new SpeedyInt(42L)));
        assertFalse(PolicyValueEquals.equals(new SpeedyText("43"), new SpeedyInt(42L)));
    }

    @Test
    void nulls_onlyEqualEachOther() {
        assertTrue(PolicyValueEquals.equals(SpeedyNull.SPEEDY_NULL, SpeedyNull.SPEEDY_NULL));
        assertFalse(PolicyValueEquals.equals(SpeedyNull.SPEEDY_NULL, new SpeedyText("")));
        assertFalse(PolicyValueEquals.equals(new SpeedyText("x"), null));
    }
}
