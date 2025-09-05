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