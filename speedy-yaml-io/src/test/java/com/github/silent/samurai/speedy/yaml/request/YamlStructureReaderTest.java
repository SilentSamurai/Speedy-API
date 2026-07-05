package com.github.silent.samurai.speedy.yaml.request;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Mirrors {@code JsonStructureReaderTest}: covers scalar leaf decode (the {@code readField}
/// switch) + ISO validation, plus the object field-walk. Since YAML's Jackson backend has no
/// JsonNode-based {@code decodeStandalone} helper here, scalars are driven straight through the
/// streaming reader (a bare-scalar YAML document positions the cursor on the value token).
@ExtendWith(MockitoExtension.class)
class YamlStructureReaderTest {

    /// Decodes a bare-scalar YAML document (e.g. {@code 123} or {@code "2024-09-10"}) as the
    /// given field's type.
    private SpeedyValue decode(FieldMetadata field, String yaml) throws SpeedyHttpException {
        YamlStructureReader reader = YamlStructureReader.over(yaml.getBytes(StandardCharsets.UTF_8));
        reader.begin(); // position on the (root) scalar token
        return reader.readField(field);
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

    @Test
    void objectFieldWalk() throws SpeedyHttpException {
        EntityMetadata entity = mock(EntityMetadata.class);
        FieldMetadata nameField = mock(FieldMetadata.class);
        when(entity.has("name")).thenReturn(true);
        when(entity.field("name")).thenReturn(nameField);
        when(nameField.getValueType()).thenReturn(ValueType.TEXT);

        YamlStructureReader reader = YamlStructureReader.over("name: Widget\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.OBJECT, reader.begin());

        FieldMetadata got = reader.nextField(entity);
        assertSame(nameField, got);
        assertEquals("Widget", reader.readField(got).asText());

        assertNull(reader.nextField(entity)); // end of object
    }
}
