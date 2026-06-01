package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnumRuleTest {

    enum Status { DRAFT, PENDING, READY }

    private final DynamicEnum statusEnum = DynamicEnum.of(Status.class);
    private final EnumRule rule = new EnumRule();

    private FieldMetadata stringEnumField() {
        FieldMetadata fm = Mockito.mock(FieldMetadata.class);
        Mockito.when(fm.isEnum()).thenReturn(true);
        Mockito.when(fm.getOperationalEnumMode()).thenReturn(EnumMode.STRING);
        Mockito.when(fm.getDynamicEnum()).thenReturn(statusEnum);
        Mockito.when(fm.getOutputPropertyName()).thenReturn("status");
        return fm;
    }

    private FieldMetadata ordinalEnumField() {
        FieldMetadata fm = Mockito.mock(FieldMetadata.class);
        Mockito.when(fm.isEnum()).thenReturn(true);
        Mockito.when(fm.getOperationalEnumMode()).thenReturn(EnumMode.ORDINAL);
        Mockito.when(fm.getDynamicEnum()).thenReturn(statusEnum);
        Mockito.when(fm.getOutputPropertyName()).thenReturn("status");
        return fm;
    }

    private FieldMetadata nonEnumField() {
        FieldMetadata fm = Mockito.mock(FieldMetadata.class);
        Mockito.when(fm.isEnum()).thenReturn(false);
        Mockito.when(fm.getOutputPropertyName()).thenReturn("name");
        return fm;
    }

    private List<String> validate(FieldMetadata fm, SpeedyValue val) {
        List<String> errors = new ArrayList<>();
        rule.validate(fm, val, errors);
        return errors;
    }

    @Test
    void stringModeWithValidSpeedyEnumShouldPass() throws Exception {
        FieldMetadata fm = stringEnumField();
        SpeedyEnum val = new SpeedyEnum("DRAFT", fm);
        assertTrue(validate(fm, val).isEmpty());
    }

    @Test
    void stringModeWithValidSpeedyTextShouldPass() {
        FieldMetadata fm = stringEnumField();
        List<String> errors = validate(fm, new SpeedyText("DRAFT"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void stringModeWithInvalidSpeedyTextShouldFail() {
        FieldMetadata fm = stringEnumField();
        List<String> errors = validate(fm, new SpeedyText("INVALID"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("invalid enum value"));
        assertTrue(errors.get(0).contains("INVALID"));
    }

    @Test
    void stringModeWithIntShouldFail() {
        FieldMetadata fm = stringEnumField();
        List<String> errors = validate(fm, new SpeedyInt(0L));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("expects a string enum value"));
    }

    @Test
    void stringModeWithDoubleShouldFail() {
        FieldMetadata fm = stringEnumField();
        List<String> errors = validate(fm, new SpeedyDouble(1.0));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("expects a string enum value"));
    }

    @Test
    void stringModeWithBooleanShouldFail() {
        FieldMetadata fm = stringEnumField();
        List<String> errors = validate(fm, new SpeedyBoolean(true));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("expects a string enum value"));
    }

    @Test
    void ordinalModeWithValidSpeedyEnumShouldPass() throws Exception {
        FieldMetadata fm = ordinalEnumField();
        SpeedyEnum val = new SpeedyEnum(0L, fm);
        assertTrue(validate(fm, val).isEmpty());
    }

    @Test
    void ordinalModeWithValidSpeedyIntShouldPass() {
        FieldMetadata fm = ordinalEnumField();
        List<String> errors = validate(fm, new SpeedyInt(1L));
        assertTrue(errors.isEmpty());
    }

    @Test
    void ordinalModeWithInvalidOrdinalShouldFail() {
        FieldMetadata fm = ordinalEnumField();
        List<String> errors = validate(fm, new SpeedyInt(99L));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("invalid enum value"));
        assertTrue(errors.get(0).contains("99"));
    }

    @Test
    void ordinalModeWithTextShouldFail() {
        FieldMetadata fm = ordinalEnumField();
        List<String> errors = validate(fm, new SpeedyText("DRAFT"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("expects an ordinal enum value"));
    }

    @Test
    void ordinalModeWithDoubleShouldFail() {
        FieldMetadata fm = ordinalEnumField();
        List<String> errors = validate(fm, new SpeedyDouble(0.0));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("expects an ordinal enum value"));
    }

    @Test
    void nonEnumFieldShouldSkipValidation() {
        FieldMetadata fm = nonEnumField();
        List<String> errors = validate(fm, new SpeedyText("hello"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void nullSpeedyValueShouldSkipValidation() {
        FieldMetadata fm = stringEnumField();
        List<String> errors = validate(fm, SpeedyNull.SPEEDY_NULL);
        assertTrue(errors.isEmpty());
    }

    @Test
    void stringModeWithNullDynamicEnumShouldPassTypeCheckButNotFailOnMembership() {
        FieldMetadata fm = Mockito.mock(FieldMetadata.class);
        Mockito.when(fm.isEnum()).thenReturn(true);
        Mockito.when(fm.getOperationalEnumMode()).thenReturn(EnumMode.STRING);
        Mockito.when(fm.getDynamicEnum()).thenReturn(null);
        Mockito.when(fm.getOutputPropertyName()).thenReturn("status");

        List<String> errors = validate(fm, new SpeedyText("DRAFT"));
        assertTrue(errors.isEmpty(), "when DynamicEnum is null, membership check is skipped");
    }

    @Test
    void ordinalModeWithNullDynamicEnumShouldPassTypeCheckButNotFailOnMembership() {
        FieldMetadata fm = Mockito.mock(FieldMetadata.class);
        Mockito.when(fm.isEnum()).thenReturn(true);
        Mockito.when(fm.getOperationalEnumMode()).thenReturn(EnumMode.ORDINAL);
        Mockito.when(fm.getDynamicEnum()).thenReturn(null);
        Mockito.when(fm.getOutputPropertyName()).thenReturn("status");

        List<String> errors = validate(fm, new SpeedyInt(0L));
        assertTrue(errors.isEmpty(), "when DynamicEnum is null, membership check is skipped");
    }
}
