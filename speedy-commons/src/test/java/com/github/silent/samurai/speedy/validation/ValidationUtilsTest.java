package com.github.silent.samurai.speedy.validation;

import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ValidationUtils} focusing on @NotNull constraint handling.
 */
class ValidationUtilsTest {

    /**
     * A simple POJO with a single @NotNull field used for validation tests.
     */
    static class Dummy {
        @NotNull
        private String name;

        Dummy() {
        }

        void setName(String name) {
            this.name = name;
        }
    }

    @Test
    void validate_shouldThrowException_whenNotNullFieldIsNull() {
        Dummy dummy = new Dummy();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.validate(dummy),
                "Expected validation to fail when @NotNull field is null");
        assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    void validate_shouldPass_whenNotNullFieldIsPopulated() {
        Dummy dummy = new Dummy();
        dummy.setName("Speedy");
        assertDoesNotThrow(() -> ValidationUtils.validate(dummy));
    }
}
