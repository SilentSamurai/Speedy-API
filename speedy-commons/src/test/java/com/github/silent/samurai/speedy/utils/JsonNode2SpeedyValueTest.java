package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.io.JsonNode2SpeedyValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JsonNode2SpeedyValueTest {

    @Test
    void jsonValueQuotedString() throws JsonProcessingException, SpeedyHttpException {
        FieldMetadata fieldMetadata = Mockito.mock(FieldMetadata.class);
        Mockito.when(fieldMetadata.getValueType()).thenReturn(ValueType.TEXT);

        JsonNode jsonNode = CommonUtil.json().readTree("\"test\"");
        SpeedyValue speedyValue = JsonNode2SpeedyValue.fromFieldMetadata(fieldMetadata, jsonNode);

        Assertions.assertTrue(speedyValue.isText());
        Assertions.assertEquals("test", speedyValue.asText());
    }

    @Test
    void jsonValueBool() throws JsonProcessingException, SpeedyHttpException {
        FieldMetadata fieldMetadata = Mockito.mock(FieldMetadata.class);
        Mockito.when(fieldMetadata.getValueType()).thenReturn(ValueType.BOOL);

        JsonNode jsonNode = CommonUtil.json().readTree("true");
        Assertions.assertTrue(jsonNode.isValueNode());
        SpeedyValue speedyValue = JsonNode2SpeedyValue.fromValueNode(fieldMetadata, (ValueNode) jsonNode);

        Assertions.assertTrue(speedyValue.isBoolean());
        Assertions.assertEquals(true, speedyValue.asBoolean());
    }

    @Test
    void jsonValueInt() throws JsonProcessingException, SpeedyHttpException {
        FieldMetadata fieldMetadata = Mockito.mock(FieldMetadata.class);
        Mockito.when(fieldMetadata.getValueType()).thenReturn(ValueType.INT);

        JsonNode jsonNode = CommonUtil.json().readTree("12");
        Assertions.assertTrue(jsonNode.isValueNode());
        SpeedyValue speedyValue = JsonNode2SpeedyValue.fromValueNode(fieldMetadata, (ValueNode) jsonNode);

        Assertions.assertTrue(speedyValue.isInt());
        Assertions.assertEquals(12, speedyValue.asInt());
    }
}