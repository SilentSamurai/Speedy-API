package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class SpeedyEnumTest {

    enum Status { DRAFT, PENDING, READY }

    private final DynamicEnum statusEnum = DynamicEnum.of(Status.class);

    private FieldMetadata stringModeField() {
        FieldMetadata fm = Mockito.mock(FieldMetadata.class);
        Mockito.when(fm.getOperationalEnumMode()).thenReturn(EnumMode.STRING);
        Mockito.when(fm.getDynamicEnum()).thenReturn(statusEnum);
        return fm;
    }

    private FieldMetadata ordinalModeField() {
        FieldMetadata fm = Mockito.mock(FieldMetadata.class);
        Mockito.when(fm.getOperationalEnumMode()).thenReturn(EnumMode.ORDINAL);
        Mockito.when(fm.getDynamicEnum()).thenReturn(statusEnum);
        return fm;
    }

    @Test
    void stringConstructorWithValidNameShouldSucceed() throws BadRequestException {
        FieldMetadata fm = stringModeField();
        SpeedyEnum e = new SpeedyEnum("DRAFT", fm);
        assertEquals("DRAFT", e.asEnum());
        assertFalse(e.isEmpty());
    }

    @Test
    void stringConstructorWithInvalidNameShouldThrowBadRequest() {
        FieldMetadata fm = stringModeField();
        BadRequestException ex = assertThrows(BadRequestException.class, () -> new SpeedyEnum("INVALID", fm));
        assertTrue(ex.getMessage().contains("INVALID"));
    }

    @Test
    void stringConstructorWithOrdinalModeShouldThrowBadRequest() {
        FieldMetadata fm = ordinalModeField();
        BadRequestException ex = assertThrows(BadRequestException.class, () -> new SpeedyEnum("DRAFT", fm));
        assertTrue(ex.getMessage().contains("expects STRING mode"));
    }

    @Test
    void longConstructorWithValidOrdinalShouldSucceed() throws BadRequestException {
        FieldMetadata fm = ordinalModeField();
        SpeedyEnum e = new SpeedyEnum(0L, fm);
        assertEquals(0L, e.asEnumOrd());
        assertEquals(0L, e.asInt());
        assertFalse(e.isEmpty());
    }

    @Test
    void longConstructorWithInvalidOrdinalShouldThrowBadRequest() {
        FieldMetadata fm = ordinalModeField();
        BadRequestException ex = assertThrows(BadRequestException.class, () -> new SpeedyEnum(99L, fm));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void longConstructorWithStringModeShouldThrowBadRequest() {
        FieldMetadata fm = stringModeField();
        BadRequestException ex = assertThrows(BadRequestException.class, () -> new SpeedyEnum(0L, fm));
        assertTrue(ex.getMessage().contains("expects ORDINAL mode"));
    }

    @Test
    void stringConstructorWithNullValueShouldThrowBadRequest() {
        FieldMetadata fm = stringModeField();
        assertThrows(BadRequestException.class, () -> new SpeedyEnum((String) null, fm));
    }

    @Test
    void longConstructorWithNullValueShouldThrowBadRequest() {
        FieldMetadata fm = ordinalModeField();
        assertThrows(BadRequestException.class, () -> new SpeedyEnum((Long) null, fm));
    }

    @Test
    void nullFieldMetadataShouldThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> new SpeedyEnum("DRAFT", null));
        assertThrows(BadRequestException.class, () -> new SpeedyEnum(0L, null));
    }

    @Test
    void stringEnumShouldHaveCorrectAsEnumAndAsText() throws BadRequestException {
        FieldMetadata fm = stringModeField();
        SpeedyEnum e = new SpeedyEnum("READY", fm);
        assertEquals("READY", e.asEnum());
        assertEquals("READY", e.asText());
        assertThrows(Exception.class, e::asInt);
        assertThrows(Exception.class, e::asEnumOrd);
    }

    @Test
    void ordinalEnumShouldHaveCorrectAsEnumOrdAndAsInt() throws BadRequestException {
        FieldMetadata fm = ordinalModeField();
        SpeedyEnum e = new SpeedyEnum(1L, fm); // PENDING
        assertEquals(1L, e.asEnumOrd());
        assertEquals(1L, e.asInt());
        assertThrows(Exception.class, e::asText);
        assertThrows(Exception.class, e::asEnum);
    }

    @Test
    void equalsAndHashCodeShouldWork() throws BadRequestException {
        FieldMetadata fm = stringModeField();
        SpeedyEnum e1 = new SpeedyEnum("DRAFT", fm);
        SpeedyEnum e2 = new SpeedyEnum("DRAFT", fm);
        SpeedyEnum e3 = new SpeedyEnum("READY", fm);

        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void toStringShouldContainDelegateValue() throws BadRequestException {
        FieldMetadata fm = stringModeField();
        SpeedyEnum e = new SpeedyEnum("DRAFT", fm);
        assertTrue(e.toString().contains("DRAFT"));
    }

    @Test
    void eachValidEnumConstantsShouldPassValidation() throws BadRequestException {
        FieldMetadata stringFm = stringModeField();
        for (Status s : Status.values()) {
            SpeedyEnum e = new SpeedyEnum(s.name(), stringFm);
            assertEquals(s.name(), e.asEnum());
        }

        FieldMetadata ordinalFm = ordinalModeField();
        for (Status s : Status.values()) {
            SpeedyEnum e = new SpeedyEnum((long) s.ordinal(), ordinalFm);
            assertEquals((long) s.ordinal(), e.asEnumOrd());
        }
    }
}
