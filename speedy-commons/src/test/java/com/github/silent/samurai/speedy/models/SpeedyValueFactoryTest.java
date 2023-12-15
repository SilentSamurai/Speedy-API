package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.io.JavaType2SpeedyValue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class SpeedyValueFactoryTest {

    @Test
    void basicToSpeedyText() throws SpeedyHttpException {
        SpeedyValue test = JavaType2SpeedyValue.convert(String.class, ValueType.TEXT, "test");
        // check if test is of SpeedyText type
        assertTrue(test instanceof SpeedyText);
        assertTrue(test.isText());
        assertEquals("test", test.asText());
    }

    @Test
    void basicToSpeedyBoolean() throws SpeedyHttpException {
        SpeedyValue test = JavaType2SpeedyValue.convert(Boolean.class, ValueType.BOOL, true);
        // check if test is of SpeedyBoolean type
        assertTrue(test instanceof SpeedyBoolean);
        assertTrue(test.isBoolean());
        assertTrue(test.asBoolean());

        SpeedyValue test1 = JavaType2SpeedyValue.convert(Boolean.class, ValueType.BOOL, false);
        // check if test is of SpeedyBoolean type
        assertTrue(test1 instanceof SpeedyBoolean);
        assertTrue(test1.isBoolean());
        assertFalse(test1.asBoolean());
    }

    @Test
    void basicToSpeedyInt() throws SpeedyHttpException {
        SpeedyValue test = JavaType2SpeedyValue.convert(Integer.class, ValueType.INT, 1234);

        assertTrue(test instanceof SpeedyInt);
        assertTrue(test.isInt());
        assertEquals(1234, test.asInt());
    }

    @Test
    void basicToSpeedyDouble() throws SpeedyHttpException {
        SpeedyValue test = JavaType2SpeedyValue.convert(Double.class, ValueType.FLOAT, 1234.099);

        assertTrue(test instanceof SpeedyDouble);
        assertTrue(test.isDouble());
        assertEquals(1234.099, test.asDouble());
    }

    @Test
    void basicToSpeedyDate() throws SpeedyHttpException {
        // create an object of Date class
        Date date = Date.from(Instant.now());
        SpeedyValue test = JavaType2SpeedyValue.convert(Date.class, ValueType.DATE, date);
        // check if test is of SpeedyText type
        assertTrue(test instanceof SpeedyDate);
        assertTrue(test.isDate());
        LocalDate date1 = test.asDate();
        // convert localDate to date
        LocalDate localDate = date.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
        assertEquals(localDate, date1);
    }

    @Test
    void basicToSpeedyDateTime() throws SpeedyHttpException {
        // create an object of Date class
        Instant datetime = Instant.now();
        SpeedyValue test = JavaType2SpeedyValue.convert(Instant.class, ValueType.DATE_TIME, datetime);
        // check if test is of SpeedyText type
        assertTrue(test instanceof SpeedyDateTime);
        assertTrue(test.isDateTime());
        LocalDateTime returnedDate = test.asDateTime();
        // convert returnedDate to Instant
        Instant instant = returnedDate.atZone(ZoneId.of("UTC")).toInstant();
        assertEquals(datetime, instant);
    }

    @Test
    void speedyValueToJavaType() {
    }
}