package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JavaTypeRegistryTest {

    private final JavaTypeRegistry registry = JavaTypeRegistry.defaults();

    // -- parseString: defaults --

    @Test
    void parseStringInteger() throws SpeedyHttpException {
        assertEquals(42, registry.parseString("42", Integer.class));
    }

    @Test
    void parseStringLong() throws SpeedyHttpException {
        assertEquals(1234567890123L, registry.parseString("1234567890123", Long.class));
    }

    @Test
    void parseStringShort() throws SpeedyHttpException {
        assertEquals((short) 32767, registry.parseString("32767", Short.class));
    }

    @Test
    void parseStringByte() throws SpeedyHttpException {
        assertEquals((byte) 127, registry.parseString("127", Byte.class));
    }

    @Test
    void parseStringFloat() throws SpeedyHttpException {
        assertEquals(3.14f, registry.parseString("3.14", Float.class), 1e-6f);
    }

    @Test
    void parseStringDouble() throws SpeedyHttpException {
        assertEquals(2.71828, registry.parseString("2.71828", Double.class), 1e-9);
    }

    @Test
    void parseStringBooleanTrue() throws SpeedyHttpException {
        assertTrue(registry.parseString("true", Boolean.class));
    }

    @Test
    void parseStringBooleanFalse() throws SpeedyHttpException {
        assertFalse(registry.parseString("false", Boolean.class));
    }

    @Test
    void parseStringString() throws SpeedyHttpException {
        assertEquals("hello world", registry.parseString("hello world", String.class));
    }

    @Test
    void parseStringUUID() throws SpeedyHttpException {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, registry.parseString(uuid.toString(), UUID.class));
    }

    @Test
    void parseStringLocalDate() throws SpeedyHttpException {
        assertEquals(LocalDate.of(2024, 1, 15), registry.parseString("2024-01-15", LocalDate.class));
    }

    @Test
    void parseStringLocalDateTime() throws SpeedyHttpException {
        assertEquals(
                LocalDateTime.of(2024, 6, 12, 14, 30, 0),
                registry.parseString("2024-06-12T14:30:00", LocalDateTime.class)
        );
    }

    @Test
    void parseStringLocalTime() throws SpeedyHttpException {
        assertEquals(
                LocalTime.of(10, 15, 30),
                registry.parseString("10:15:30", LocalTime.class)
        );
    }

    @Test
    void parseStringZonedDateTime() throws SpeedyHttpException {
        assertEquals(
                ZonedDateTime.of(2024, 12, 25, 0, 0, 0, 0, ZoneOffset.UTC),
                registry.parseString("2024-12-25T00:00:00Z", ZonedDateTime.class)
        );
    }

    @Test
    void parseStringInstant() throws SpeedyHttpException {
        assertEquals(
                Instant.parse("2024-06-12T12:00:00Z"),
                registry.parseString("2024-06-12T12:00:00Z", Instant.class)
        );
    }

    // -- parseString: null / edge cases --

    @Test
    void parseStringNullReturnsNull() throws SpeedyHttpException {
        assertNull(registry.parseString(null, String.class));
    }

    @Test
    void parseStringEmptyString() throws SpeedyHttpException {
        assertEquals("", registry.parseString("", String.class));
    }

    @Test
    void parseStringPrimitiveInt() throws SpeedyHttpException {
        assertEquals(99, registry.parseString("99", int.class));
    }

    @Test
    void parseStringPrimitiveBoolean() throws SpeedyHttpException {
        assertTrue(registry.parseString("true", boolean.class));
    }

    @Test
    void parseStringUnregisteredClassThrows() {
        assertThrows(ConversionException.class,
                () -> registry.parseString("x", StringBuilder.class));
    }

    // -- custom registerFromString --

    @Test
    void customFromStringConverter() throws SpeedyHttpException {
        JavaTypeRegistry r = new JavaTypeRegistry(null);
        r.registerFromString(StringBuilder.class, StringBuilder::new);
        StringBuilder sb = r.parseString("custom", StringBuilder.class);
        assertEquals("custom", sb.toString());
    }

    // -- parent fallback --

    @Test
    void parseStringFallsBackToParent() throws SpeedyHttpException {
        JavaTypeRegistry parent = new JavaTypeRegistry(null);
        parent.registerFromString(Integer.class, Integer::parseInt);
        JavaTypeRegistry child = new JavaTypeRegistry(parent);
        assertEquals(7, child.parseString("7", Integer.class));
    }

    @Test
    void parseStringNoParentThrows() {
        JavaTypeRegistry orphan = new JavaTypeRegistry(null);
        assertThrows(ConversionException.class,
                () -> orphan.parseString("1", Integer.class));
    }

    @Test
    void customConverterOverridesParent() throws SpeedyHttpException {
        JavaTypeRegistry parent = new JavaTypeRegistry(null);
        parent.registerFromString(String.class, v -> "parent:" + v);
        JavaTypeRegistry child = new JavaTypeRegistry(parent);
        child.registerFromString(String.class, v -> "child:" + v);
        assertEquals("child:x", child.parseString("x", String.class));
    }

    // -- toJava / toSpeedy round trips --

    @Test
    void toJavaInteger() throws SpeedyHttpException {
        assertEquals(42, registry.toJava(new SpeedyInt(42L), Integer.class));
    }

    @Test
    void toJavaString() throws SpeedyHttpException {
        assertEquals("hello", registry.toJava(new SpeedyText("hello"), String.class));
    }

    @Test
    void toJavaBoolean() throws SpeedyHttpException {
        assertTrue(registry.toJava(new SpeedyBoolean(true), Boolean.class));
    }

    @Test
    void toJavaLocalDate() throws SpeedyHttpException {
        LocalDate date = LocalDate.of(2024, 6, 1);
        assertEquals(date, registry.toJava(new SpeedyDate(date), LocalDate.class));
    }

    @Test
    void toJavaLocalDateTime() throws SpeedyHttpException {
        LocalDateTime dt = LocalDateTime.of(2024, 6, 1, 10, 30);
        assertEquals(dt, registry.toJava(new SpeedyDateTime(dt), LocalDateTime.class));
    }

    @Test
    void toJavaDouble() throws SpeedyHttpException {
        assertEquals(3.14, registry.toJava(new SpeedyDouble(3.14), Double.class), 1e-9);
    }

    @Test
    void toJavaNullReturnsNull() throws SpeedyHttpException {
        assertNull(registry.toJava(null, String.class));
        assertNull(registry.toJava(SpeedyNull.SPEEDY_NULL, String.class));
    }

    @Test
    void toJavaEnumByText() throws SpeedyHttpException {
        JavaTypeRegistry r = new JavaTypeRegistry(null);
        r.register(TestEnum.class, ValueType.TEXT,
                sv -> TestEnum.valueOf(sv.asText()),
                val -> new SpeedyText(((TestEnum) val).name()));
        assertEquals(TestEnum.A, r.toJava(new SpeedyText("A"), TestEnum.class));
    }

    @Test
    void toJavaUnregisteredClassThrows() {
        assertThrows(ConversionException.class,
                () -> registry.toJava(new SpeedyText("x"), StringBuilder.class));
    }

    @Test
    void toSpeedyString() throws SpeedyHttpException {
        SpeedyValue sv = registry.toSpeedy("hello", ValueType.TEXT);
        assertInstanceOf(SpeedyText.class, sv);
        assertEquals("hello", sv.asText());
    }

    @Test
    void toSpeedyLong() throws SpeedyHttpException {
        SpeedyValue sv = registry.toSpeedy(42L, ValueType.INT);
        assertInstanceOf(SpeedyInt.class, sv);
        assertEquals(42L, sv.asLong());
    }

    @Test
    void toSpeedyNullReturnsNull() throws SpeedyHttpException {
        assertInstanceOf(SpeedyNull.class, registry.toSpeedy(null, ValueType.TEXT));
    }

    @Test
    void toSpeedyLocalDate() throws SpeedyHttpException {
        LocalDate date = LocalDate.of(2024, 1, 1);
        SpeedyValue sv = registry.toSpeedy(date, ValueType.DATE);
        assertInstanceOf(SpeedyDate.class, sv);
        assertEquals(date, sv.asDate());
    }

    @Test
    void toSpeedyEnumByText() throws SpeedyHttpException {
        JavaTypeRegistry r = new JavaTypeRegistry(null);
        r.register(TestEnum.class, ValueType.TEXT,
                sv -> TestEnum.valueOf(sv.asText()),
                val -> new SpeedyText(((TestEnum) val).name()));
        SpeedyValue sv = r.toSpeedy(TestEnum.B, ValueType.TEXT);
        assertInstanceOf(SpeedyText.class, sv);
        assertEquals("B", sv.asText());
    }

    @Test
    void toSpeedyUnregisteredClassThrows() {
        assertThrows(ConversionException.class,
                () -> registry.toSpeedy(new Object(), ValueType.TEXT));
    }

    // -- canToJava / canToSpeedy --

    @Test
    void canToJavaReturnsTrueForRegistered() {
        assertTrue(registry.canToJava(ValueType.TEXT, String.class));
        assertTrue(registry.canToJava(ValueType.INT, Integer.class));
        assertTrue(registry.canToJava(ValueType.DATE, LocalDate.class));
    }

    @Test
    void canToJavaReturnsTrueForEnum() {
        assertTrue(registry.canToJava(ValueType.TEXT, TestEnum.class));
    }

    @Test
    void canToJavaReturnsFalseForUnknown() {
        assertFalse(registry.canToJava(ValueType.TEXT, JavaTypeRegistry.class));
    }

    @Test
    void canToSpeedyReturnsTrueForRegistered() {
        assertTrue(registry.canToSpeedy(ValueType.TEXT, String.class));
        assertTrue(registry.canToSpeedy(ValueType.INT, Integer.class));
    }

    @Test
    void canToSpeedyReturnsTrueForEnum() {
        assertTrue(registry.canToSpeedy(ValueType.TEXT, TestEnum.class));
    }

    @Test
    void canToSpeedyReturnsFalseForUnknown() {
        assertFalse(registry.canToSpeedy(ValueType.TEXT, JavaTypeRegistry.class));
    }

    // -- helper types --

    enum TestEnum { A, B, C }
}
