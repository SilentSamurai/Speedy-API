package com.github.silent.samurai.speedy.json.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Leaf decoding (the {@code readField} switch) lives on {@link JsonStructureReader};
/// these cover scalar decode + ISO validation via {@code decodeStandalone}, which the
/// query parser also uses for filter literals.
@ExtendWith(MockitoExtension.class)
class JsonStructureReaderTest {

    private SpeedyValue decode(FieldMetadata field, String json) throws SpeedyHttpException {
        JsonNode node = readNode(json);
        return JsonStructureReader.decodeStandalone(node, field);
    }

    private JsonNode readNode(String json) {
        try {
            return CommonUtil.json().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void text() throws SpeedyHttpException {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.TEXT);
        SpeedyValue value = decode(field, "\"Sample Text\"");
        assertTrue(value.isText());
        assertEquals("Sample Text", value.asText());
    }

    @Test
    void integer() throws SpeedyHttpException {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.INT);
        SpeedyValue value = decode(field, "123");
        assertTrue(value.isInt());
        assertEquals(123, value.asInt());
    }

    @Test
    void bool() throws SpeedyHttpException {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.BOOL);
        SpeedyValue value = decode(field, "true");
        assertTrue(value.isBoolean());
        assertEquals(true, value.asBoolean());
    }

    @Test
    void date() throws SpeedyHttpException {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.DATE);
        SpeedyValue value = decode(field, "\"2024-09-10\"");
        assertEquals(LocalDate.of(2024, 9, 10), value.asDate());
    }

    @Test
    void invalidDateFormat() {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.DATE);
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> decode(field, "\"invalid-date\""));
        assertTrue(exception.getMessage().contains("Date value must be a string with ISO_DATE"));
    }

    @Test
    void nullValue() throws SpeedyHttpException {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.TEXT);
        SpeedyValue value = decode(field, "null");
        assertTrue(value.isNull());
    }
}
