package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class SpeedyValueFactoryTest {

    @Test
    void basicToSpeedyText() throws SpeedyHttpException {
        SpeedyValue test = SpeedyValueFactory.fromJavaTypes(String.class, ValueType.TEXT, "test");
        // check if test is of SpeedyText type
        assertTrue(test instanceof SpeedyText);
        assertTrue(test.isText());
        assertEquals("test", test.asText());
    }

    @Test
    void basicToSpeedyBoolean() throws SpeedyHttpException {
        SpeedyValue test = SpeedyValueFactory.fromJavaTypes(Boolean.class, ValueType.BOOL, true);
        // check if test is of SpeedyBoolean type
        assertTrue(test instanceof SpeedyBoolean);
        assertTrue(test.isBoolean());
        assertTrue(test.asBoolean());

        SpeedyValue test1 = SpeedyValueFactory.fromJavaTypes(Boolean.class, ValueType.BOOL, false);
        // check if test is of SpeedyBoolean type
        assertTrue(test1 instanceof SpeedyBoolean);
        assertTrue(test1.isBoolean());
        assertFalse(test1.asBoolean());
    }

    @Test
    void basicToSpeedyInt() throws SpeedyHttpException {
        SpeedyValue test = SpeedyValueFactory.fromJavaTypes(Integer.class, ValueType.INT, 1234);

        assertTrue(test instanceof SpeedyInt);
        assertTrue(test.isInt());
        assertEquals(1234, test.asInt());
    }

    @Test
    void basicToSpeedyDouble() throws SpeedyHttpException {
        SpeedyValue test = SpeedyValueFactory.fromJavaTypes(Double.class, ValueType.FLOAT, 1234.099);

        assertTrue(test instanceof SpeedyDouble);
        assertTrue(test.isDouble());
        assertEquals(1234.099, test.asDouble());
    }

    @Test
    void basicToSpeedyDate() throws SpeedyHttpException {
        // create an object of Date class
        Date date = Date.from(Instant.now());
        SpeedyValue test = SpeedyValueFactory.fromJavaTypes(Date.class, ValueType.DATE, date);
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
        SpeedyValue test = SpeedyValueFactory.fromJavaTypes(Instant.class, ValueType.DATE_TIME, datetime);
        // check if test is of SpeedyText type
        assertTrue(test instanceof SpeedyDateTime);
        assertTrue(test.isDateTime());
        LocalDateTime returnedDate = test.asDateTime();
        // convert returnedDate to Instant
        Instant instant = returnedDate.atZone(ZoneId.of("UTC")).toInstant();
        assertEquals(datetime, instant);
    }

    @Test
    void jsonValueQuotedString() throws JsonProcessingException, SpeedyHttpException {
        FieldMetadata fieldMetadata = Mockito.mock(FieldMetadata.class);
        Mockito.when(fieldMetadata.getValueType()).thenReturn(ValueType.TEXT);

        JsonNode jsonNode = CommonUtil.json().readTree("\"test\"");
        SpeedyValue speedyValue = SpeedyValueFactory.fromJsonNode(fieldMetadata, jsonNode);

        Assertions.assertTrue(speedyValue.isText());
        Assertions.assertEquals("test", speedyValue.asText());
    }

    @Test
    void jsonValueBool() throws JsonProcessingException, SpeedyHttpException {
        FieldMetadata fieldMetadata = Mockito.mock(FieldMetadata.class);
        Mockito.when(fieldMetadata.getValueType()).thenReturn(ValueType.BOOL);

        JsonNode jsonNode = CommonUtil.json().readTree("true");
        Assertions.assertTrue(jsonNode.isValueNode());
        SpeedyValue speedyValue = SpeedyValueFactory.fromJsonValue(fieldMetadata, (ValueNode) jsonNode);

        Assertions.assertTrue(speedyValue.isBoolean());
        Assertions.assertEquals(true, speedyValue.asBoolean());
    }

    @Test
    void jsonValueInt() throws JsonProcessingException, SpeedyHttpException {
        FieldMetadata fieldMetadata = Mockito.mock(FieldMetadata.class);
        Mockito.when(fieldMetadata.getValueType()).thenReturn(ValueType.INT);

        JsonNode jsonNode = CommonUtil.json().readTree("12");
        Assertions.assertTrue(jsonNode.isValueNode());
        SpeedyValue speedyValue = SpeedyValueFactory.fromJsonValue(fieldMetadata, (ValueNode) jsonNode);

        Assertions.assertTrue(speedyValue.isInt());
        Assertions.assertEquals(12, speedyValue.asInt());
    }
}