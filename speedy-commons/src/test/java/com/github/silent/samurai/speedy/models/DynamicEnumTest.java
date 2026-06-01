package com.github.silent.samurai.speedy.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DynamicEnumTest {

    enum Color { RED, GREEN, BLUE }

    private final DynamicEnum colorEnum = DynamicEnum.of(Color.class);

    @Test
    void fromNameWithValidNameShouldReturnPresent() {
        assertTrue(colorEnum.fromName("RED").isPresent());
        assertEquals("RED", colorEnum.fromName("RED").get().name());
        assertEquals(0, colorEnum.fromName("RED").get().code());
    }

    @Test
    void fromNameWithInvalidNameShouldReturnEmpty() {
        assertTrue(colorEnum.fromName("PURPLE").isEmpty());
    }

    @Test
    void fromNameWithNullShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> colorEnum.fromName(null));
    }

    @Test
    void fromNameIsCaseSensitive() {
        assertTrue(colorEnum.fromName("red").isEmpty());
    }

    @Test
    void fromCodeWithValidCodeShouldReturnPresent() {
        assertTrue(colorEnum.fromCode(1).isPresent());
        assertEquals("GREEN", colorEnum.fromCode(1).get().name());
    }

    @Test
    void fromCodeWithInvalidCodeShouldReturnEmpty() {
        assertTrue(colorEnum.fromCode(99).isEmpty());
    }

    @Test
    void fromCodeWithNegativeCodeShouldReturnEmpty() {
        assertTrue(colorEnum.fromCode(-1).isEmpty());
    }

    @Test
    void valuesShouldReturnAllVariants() {
        assertEquals(3, colorEnum.values().size());
        assertEquals("RED", colorEnum.values().get(0).name());
        assertEquals("GREEN", colorEnum.values().get(1).name());
        assertEquals("BLUE", colorEnum.values().get(2).name());
    }

    @Test
    void factoryMethodShouldMapAllOrdinalsCorrectly() {
        for (Color c : Color.values()) {
            assertTrue(colorEnum.fromName(c.name()).isPresent());
            assertTrue(colorEnum.fromCode(c.ordinal()).isPresent());
            assertEquals(c.name(), colorEnum.fromCode(c.ordinal()).get().name());
        }
    }
}
