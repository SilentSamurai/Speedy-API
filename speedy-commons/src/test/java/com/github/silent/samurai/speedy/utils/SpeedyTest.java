package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import com.github.silent.samurai.speedy.models.SpeedyDate;
import com.github.silent.samurai.speedy.models.SpeedyDateTime;
import com.github.silent.samurai.speedy.models.SpeedyDouble;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.models.SpeedyTime;
import com.github.silent.samurai.speedy.models.SpeedyZonedDateTime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Covers {@link Speedy#from(Object)} — the runtime-type dispatcher used where no
/// {@code FieldMetadata} is available to drive conversion (policy variables, for instance).
class SpeedyTest {

    @Test
    void from_text() {
        SpeedyValue value = Speedy.from((Object) "abc");
        assertInstanceOf(SpeedyText.class, value);
        assertEquals("abc", value.asText());
    }

    @Test
    void from_boolean() {
        SpeedyValue value = Speedy.from((Object) Boolean.TRUE);
        assertInstanceOf(SpeedyBoolean.class, value);
        assertEquals(true, value.asBoolean());
    }

    @Test
    void from_longAndInteger_bothBecomeSpeedyInt() {
        SpeedyValue fromLong = Speedy.from((Object) 7L);
        SpeedyValue fromInteger = Speedy.from((Object) 7);

        assertInstanceOf(SpeedyInt.class, fromLong);
        assertInstanceOf(SpeedyInt.class, fromInteger);
        assertEquals(7L, fromLong.asInt());
        assertEquals(7L, fromInteger.asInt());
    }

    @Test
    void from_doubleAndFloat_bothBecomeSpeedyDouble() {
        SpeedyValue fromDouble = Speedy.from((Object) 1.5d);
        SpeedyValue fromFloat = Speedy.from((Object) 1.5f);

        assertInstanceOf(SpeedyDouble.class, fromDouble);
        assertInstanceOf(SpeedyDouble.class, fromFloat);
        assertEquals(1.5d, fromDouble.asDouble());
        assertEquals(1.5d, fromFloat.asDouble());
    }

    @Test
    void from_temporalTypes() {
        LocalDate date = LocalDate.of(2020, 1, 2);
        LocalTime time = LocalTime.of(10, 30);
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of("UTC"));

        assertInstanceOf(SpeedyDate.class, Speedy.from((Object) date));
        assertInstanceOf(SpeedyTime.class, Speedy.from((Object) time));
        assertInstanceOf(SpeedyDateTime.class, Speedy.from((Object) dateTime));
        assertInstanceOf(SpeedyZonedDateTime.class, Speedy.from((Object) zonedDateTime));

        assertEquals(date, Speedy.from((Object) date).asDate());
        assertEquals(time, Speedy.from((Object) time).asTime());
        assertEquals(dateTime, Speedy.from((Object) dateTime).asDateTime());
        assertEquals(zonedDateTime, Speedy.from((Object) zonedDateTime).asZonedDateTime());
    }

    @Test
    void from_null_becomesSpeedyNull() {
        SpeedyValue value = Speedy.from((Object) null);
        assertInstanceOf(SpeedyNull.class, value);
        assertTrue(value.isNull());
    }

    @Test
    void from_speedyValue_passesThroughUnchanged() {
        SpeedyText original = new SpeedyText("abc");
        assertSame(original, Speedy.from((Object) original));
    }

    @Test
    void from_unsupportedType_throwsAndNamesTheType() {
        ConversionException thrown = assertThrows(ConversionException.class,
                () -> Speedy.from(new Object()));
        assertTrue(thrown.getMessage().contains("java.lang.Object"), thrown.getMessage());
    }

    @Test
    void from_ambiguousNumericTypes_areRejectedRatherThanGuessed() {
        // No field metadata means no way to tell FLOAT from INT — fail instead of guessing.
        assertThrows(ConversionException.class, () -> Speedy.from(BigDecimal.ONE));
        assertThrows(ConversionException.class, () -> Speedy.from(BigInteger.ONE));
    }

    @Test
    void from_collection_isUnsupported() {
        assertThrows(ConversionException.class, () -> Speedy.from(List.of("a")));
    }
}
