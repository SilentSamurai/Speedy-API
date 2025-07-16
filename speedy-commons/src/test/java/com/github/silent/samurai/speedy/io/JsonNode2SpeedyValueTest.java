package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonNode2SpeedyValueTest {

    private KeyFieldMetadata mockKeyFieldMetadata;
    private FieldMetadata mockFieldMetadata;
    private EntityMetadata mockEntityMetadata;
    private ObjectNode jsonNode;

    @BeforeEach
    void setUp() {
        mockKeyFieldMetadata = mock(KeyFieldMetadata.class);
        mockFieldMetadata = mock(FieldMetadata.class);
        mockEntityMetadata = mock(EntityMetadata.class);
        jsonNode = JsonNodeFactory.instance.objectNode();
    }

    @Test
    void testFromValueNode_Text() throws BadRequestException {
        when(mockFieldMetadata.getValueType()).thenReturn(ValueType.TEXT);
        ValueNode textNode = JsonNodeFactory.instance.textNode("Sample Text");
        SpeedyValue result = JsonNode2SpeedyValue.fromValueNode(mockFieldMetadata, textNode);
        assertEquals("Sample Text", result.asText());
    }

    @Test
    void testFromValueNode_Int() throws BadRequestException {
        when(mockFieldMetadata.getValueType()).thenReturn(ValueType.INT);
        ValueNode intNode = JsonNodeFactory.instance.numberNode(123L);
        SpeedyValue result = JsonNode2SpeedyValue.fromValueNode(mockFieldMetadata, intNode);
        assertEquals(123, result.asInt());
    }

    @Test
    void testFromValueNode_Bool() throws BadRequestException {
        when(mockFieldMetadata.getValueType()).thenReturn(ValueType.BOOL);
        ValueNode boolNode = JsonNodeFactory.instance.booleanNode(true);
        SpeedyValue result = JsonNode2SpeedyValue.fromValueNode(mockFieldMetadata, boolNode);
        assertEquals(true, result.asBoolean());
    }

    @Test
    void testFromValueNode_Date() throws BadRequestException {
        when(mockFieldMetadata.getValueType()).thenReturn(ValueType.DATE);
        ValueNode dateNode = JsonNodeFactory.instance.textNode("2024-09-10");
        SpeedyValue result = JsonNode2SpeedyValue.fromValueNode(mockFieldMetadata, dateNode);
        assertEquals(LocalDate.of(2024, 9, 10), result.asDate());
    }

    @Test
    void testFromValueNode_InvalidDateFormat() {
        when(mockFieldMetadata.getValueType()).thenReturn(ValueType.DATE);
        ValueNode invalidDateNode = JsonNodeFactory.instance.textNode("invalid-date");
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            JsonNode2SpeedyValue.fromValueNode(mockFieldMetadata, invalidDateNode);
        });
        assertTrue(exception.getMessage().contains("Date value must be a string with ISO_DATE"));
    }

    @Test
    void testFromFieldMetadata_Collection() throws SpeedyHttpException {
        when(mockFieldMetadata.isCollection()).thenReturn(true);
        when(mockFieldMetadata.getValueType()).thenReturn(ValueType.INT);

        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(123);

        SpeedyValue result = JsonNode2SpeedyValue.fromFieldMetadata(mockFieldMetadata, arrayNode);
        assertNotNull(result);
        assertTrue(result.isCollection());
        assertEquals(123, result.asCollection().stream().findAny().get().asInt());
    }

    @Test
    void testFromEntityMetadata_Valid() throws SpeedyHttpException {
        when(mockEntityMetadata.getAllFields()).thenReturn(Set.of(mockFieldMetadata));
        when(mockFieldMetadata.getOutputPropertyName()).thenReturn("name");
        when(mockFieldMetadata.getValueType()).thenReturn(ValueType.TEXT);
        jsonNode.put("name", "Test Entity");

        SpeedyEntity result = JsonNode2SpeedyValue.fromEntityMetadata(mockEntityMetadata, jsonNode);
        assertNotNull(result);
    }

    @Test
    void testFromPkJson_MissingKeyField() {
        when(mockEntityMetadata.getKeyFields()).thenReturn(Set.of(mockKeyFieldMetadata));
        when(mockKeyFieldMetadata.getOutputPropertyName()).thenReturn("id");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            JsonNode2SpeedyValue.fromPkJson(mockEntityMetadata, jsonNode);
        });
        assertTrue(exception.getMessage().contains("Missing key field"));
    }
}
