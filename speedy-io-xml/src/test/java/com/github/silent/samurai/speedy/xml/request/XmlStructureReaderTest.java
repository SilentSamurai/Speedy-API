package com.github.silent.samurai.speedy.xml.request;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XmlStructureReaderTest {

    @Test
    void text() throws Exception {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.TEXT);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("name")).thenReturn(true);
        when(entity.field("name")).thenReturn(field);

        String xml = "<root><name>Sample Text</name></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.OBJECT, reader.begin());

        FieldMetadata got = reader.nextField(entity);
        assertSame(field, got);
        assertEquals("Sample Text", reader.readField(got).asText());
    }

    @Test
    void integer() throws Exception {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.INT);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("count")).thenReturn(true);
        when(entity.field("count")).thenReturn(field);

        String xml = "<root><count>123</count></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();
        FieldMetadata got = reader.nextField(entity);
        SpeedyValue value = reader.readField(got);
        assertTrue(value.isInt());
        assertEquals(123, value.asInt());
    }

    @Test
    void bool() throws Exception {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.BOOL);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("active")).thenReturn(true);
        when(entity.field("active")).thenReturn(field);

        String xml = "<root><active>true</active></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();
        FieldMetadata got = reader.nextField(entity);
        SpeedyValue value = reader.readField(got);
        assertTrue(value.isBoolean());
        assertEquals(true, value.asBoolean());
    }

    @Test
    void date() throws Exception {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.DATE);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("date")).thenReturn(true);
        when(entity.field("date")).thenReturn(field);

        String xml = "<root><date>2024-09-10</date></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();
        FieldMetadata got = reader.nextField(entity);
        assertEquals(LocalDate.of(2024, 9, 10), reader.readField(got).asDate());
    }

    @Test
    void invalidDateFormat() throws Exception {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getValueType()).thenReturn(ValueType.DATE);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("date")).thenReturn(true);
        when(entity.field("date")).thenReturn(field);

        String xml = "<root><date>invalid-date</date></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();
        FieldMetadata got = reader.nextField(entity);
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> reader.readField(got));
        assertTrue(exception.getMessage().contains("Date value must be a string with ISO_DATE"));
    }

    @Test
    void nullValue() throws Exception {
        FieldMetadata field = mock(FieldMetadata.class);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("name")).thenReturn(true);
        when(entity.field("name")).thenReturn(field);

        String xml = "<root><name></name></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();
        FieldMetadata got = reader.nextField(entity);
        assertTrue(reader.readField(got).isNull());
    }

    @Test
    void objectFieldWalk() throws Exception {
        EntityMetadata entity = mock(EntityMetadata.class);
        FieldMetadata nameField = mock(FieldMetadata.class);
        when(entity.has("name")).thenReturn(true);
        when(entity.field("name")).thenReturn(nameField);
        when(nameField.getValueType()).thenReturn(ValueType.TEXT);

        String xml = "<root><name>Widget</name></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.OBJECT, reader.begin());

        FieldMetadata got = reader.nextField(entity);
        assertSame(nameField, got);
        assertEquals("Widget", reader.readField(got).asText());

        assertNull(reader.nextField(entity));
    }

    @Test
    void arrayDetection() throws Exception {
        FieldMetadata itemField = mock(FieldMetadata.class);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("item")).thenReturn(true);
        when(entity.field("item")).thenReturn(itemField);

        String xml = "<root><item>A</item><item>B</item></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.OBJECT, reader.begin());

        FieldMetadata got = reader.nextField(entity);
        assertNotNull(got);
        assertEquals(StructureReader.Kind.ARRAY, reader.currentKind());
    }

    @Test
    void objectNesting() throws Exception {
        FieldMetadata childField = mock(FieldMetadata.class);
        EntityMetadata parentEntity = mock(EntityMetadata.class);
        when(parentEntity.has("child")).thenReturn(true);
        when(parentEntity.field("child")).thenReturn(childField);

        FieldMetadata nameField = mock(FieldMetadata.class);
        when(nameField.getValueType()).thenReturn(ValueType.TEXT);
        EntityMetadata childEntity = mock(EntityMetadata.class);
        when(childEntity.has("name")).thenReturn(true);
        when(childEntity.field("name")).thenReturn(nameField);

        String xml = "<root><child><name>Nested</name></child></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();

        FieldMetadata got = reader.nextField(parentEntity);
        assertNotNull(got);
        assertEquals(StructureReader.Kind.OBJECT, reader.currentKind());

        FieldMetadata innerField = reader.nextField(childEntity);
        assertSame(nameField, innerField);
        assertEquals("Nested", reader.readField(innerField).asText());
    }

    @Test
    void skipUnknownField() throws Exception {
        EntityMetadata entity = mock(EntityMetadata.class);
        FieldMetadata knownField = mock(FieldMetadata.class);
        when(entity.has("known")).thenReturn(true);
        when(entity.has("unknown")).thenReturn(false);
        when(entity.field("known")).thenReturn(knownField);
        when(knownField.getValueType()).thenReturn(ValueType.TEXT);

        String xml = "<root><unknown>skip-me</unknown><known>hello</known></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();

        FieldMetadata got = reader.nextField(entity);
        assertSame(knownField, got);
        assertEquals("hello", reader.readField(got).asText());
    }

    @Test
    void textValueForQueryParser() throws Exception {
        String xml = "<root><from>Product</from></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();

        String key = reader.nextKey();
        assertEquals("from", key);
        String val = reader.textValue();
        assertNotNull(val);
        assertEquals("Product", val.strip());
    }

    @Test
    void rootArrayWithEntityWrapperSingle() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><entity><name>Widget</name></entity></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.ARRAY, reader.begin());
    }

    @Test
    void rootArrayWithEntityWrapperSingleNoDecl() throws Exception {
        String xml = "<root><entity><name>Widget</name></entity></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.ARRAY, reader.begin());
    }

    @Test
    void rootArrayWithEntityWrapperBulk() throws Exception {
        String xml = "<root><entity><name>A</name></entity><entity><name>B</name></entity><entity><name>C</name></entity></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.ARRAY, reader.begin());
    }

    @Test
    void rootObjectWithDifferentFields() throws Exception {
        String xml = "<root><id>1</id><name>Widget</name></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.OBJECT, reader.begin());
    }

    @Test
    void rootObjectWithSingleTextChild() throws Exception {
        String xml = "<root><name>Widget</name></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.OBJECT, reader.begin());
    }

    @Test
    void walkEntityArraySingle() throws Exception {
        FieldMetadata nameField = mock(FieldMetadata.class);
        when(nameField.getValueType()).thenReturn(ValueType.TEXT);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("name")).thenReturn(true);
        when(entity.field("name")).thenReturn(nameField);

        String xml = "<root><entity><name>Widget</name></entity></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.ARRAY, reader.begin());

        assertEquals(StructureReader.Kind.OBJECT, reader.nextElement());

        FieldMetadata got = reader.nextField(entity);
        assertSame(nameField, got);
        assertEquals("Widget", reader.readField(got).asText());

        assertNull(reader.nextField(entity));
        assertNull(reader.nextElement());
    }

    @Test
    void walkEntityArrayBulk() throws Exception {
        FieldMetadata nameField = mock(FieldMetadata.class);
        when(nameField.getValueType()).thenReturn(ValueType.TEXT);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("name")).thenReturn(true);
        when(entity.field("name")).thenReturn(nameField);

        String xml = "<root><entity><name>A</name></entity><entity><name>B</name></entity><entity><name>C</name></entity></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.ARRAY, reader.begin());

        int count = 0;
        while (reader.nextElement() != null) {
            FieldMetadata got = reader.nextField(entity);
            assertNotNull(got);
            reader.readField(got);
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    void walkDeleteBody() throws Exception {
        FieldMetadata idField = mock(FieldMetadata.class);
        when(idField.getValueType()).thenReturn(ValueType.TEXT);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("id")).thenReturn(true);
        when(entity.field("id")).thenReturn(idField);

        String xml = "<root><entity><id>1</id></entity><entity><id>2</id></entity></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.ARRAY, reader.begin());

        int count = 0;
        while (reader.nextElement() != null) {
            FieldMetadata got = reader.nextField(entity);
            assertNotNull(got);
            reader.readField(got);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void walkUpdateBody() throws Exception {
        FieldMetadata idField = mock(FieldMetadata.class);
        when(idField.getValueType()).thenReturn(ValueType.TEXT);
        FieldMetadata nameField = mock(FieldMetadata.class);
        when(nameField.getValueType()).thenReturn(ValueType.TEXT);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("id")).thenReturn(true);
        when(entity.has("name")).thenReturn(true);
        when(entity.field("id")).thenReturn(idField);
        when(entity.field("name")).thenReturn(nameField);

        String xml = "<root><id>1</id><name>Updated</name></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(StructureReader.Kind.OBJECT, reader.begin());

        FieldMetadata got1 = reader.nextField(entity);
        assertSame(idField, got1);
        assertEquals("1", reader.readField(got1).asText());

        FieldMetadata got2 = reader.nextField(entity);
        assertSame(nameField, got2);
        assertEquals("Updated", reader.readField(got2).asText());

        assertNull(reader.nextField(entity));
    }

    @Test
    void scalarArrayWrapperForm() throws Exception {
        String xml = "<root><tags><item>a</item><item>b</item></tags></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();

        assertEquals("tags", reader.nextKey());
        assertEquals(StructureReader.Kind.ARRAY, reader.currentKind());
        assertEquals(StructureReader.Kind.VALUE, reader.nextElement());
        assertEquals("a", reader.textValue());
        assertEquals(StructureReader.Kind.VALUE, reader.nextElement());
        assertEquals("b", reader.textValue());
        assertNull(reader.nextElement());
    }

    @Test
    void scalarArrayWrapperSingleItem() throws Exception {
        String xml = "<root><in><item>a</item></in></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();

        assertEquals("in", reader.nextKey());
        assertEquals(StructureReader.Kind.ARRAY, reader.currentKind());
        assertEquals(StructureReader.Kind.VALUE, reader.nextElement());
        assertEquals("a", reader.textValue());
        assertNull(reader.nextElement());
    }

    @Test
    void scalarArraySiblingForm() throws Exception {
        String xml = "<root><tag>a</tag><tag>b</tag></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();

        assertEquals("tag", reader.nextKey());
        assertEquals(StructureReader.Kind.ARRAY, reader.currentKind());
        assertEquals(StructureReader.Kind.VALUE, reader.nextElement());
        assertEquals("a", reader.textValue());
        assertEquals(StructureReader.Kind.VALUE, reader.nextElement());
        assertEquals("b", reader.textValue());
        assertNull(reader.nextElement());
        assertNull(reader.nextKey());
    }

    @Test
    void singleItemNestedCollectionMarker() throws Exception {
        FieldMetadata itemsField = mock(FieldMetadata.class);
        when(itemsField.isCollection()).thenReturn(true);
        EntityMetadata parent = mock(EntityMetadata.class);
        when(parent.has("items")).thenReturn(true);
        when(parent.field("items")).thenReturn(itemsField);

        FieldMetadata fField = mock(FieldMetadata.class);
        when(fField.getValueType()).thenReturn(ValueType.INT);
        EntityMetadata itemEntity = mock(EntityMetadata.class);
        when(itemEntity.has("f")).thenReturn(true);
        when(itemEntity.field("f")).thenReturn(fField);

        String xml = "<root><items><entity><f>1</f></entity></items></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();

        FieldMetadata got = reader.nextField(parent);
        assertSame(itemsField, got);
        assertEquals(StructureReader.Kind.ARRAY, reader.currentKind());

        assertEquals(StructureReader.Kind.OBJECT, reader.nextElement());
        FieldMetadata inner = reader.nextField(itemEntity);
        assertSame(fField, inner);
        assertEquals(1, reader.readField(inner).asInt());
    }

    @Test
    void singleItemCollectionViaMetadataHint() throws Exception {
        FieldMetadata tagsField = mock(FieldMetadata.class);
        when(tagsField.isCollection()).thenReturn(true);
        EntityMetadata entity = mock(EntityMetadata.class);
        when(entity.has("tags")).thenReturn(true);
        when(entity.field("tags")).thenReturn(tagsField);

        // A single item whose element name is NOT a reserved marker: only the collection hint
        // distinguishes it from a plain nested object.
        String xml = "<root><tags><widget>a</widget></tags></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();

        FieldMetadata got = reader.nextField(entity);
        assertSame(tagsField, got);
        assertEquals(StructureReader.Kind.ARRAY, reader.currentKind());
    }

    @Test
    void mixedChildrenIsObject() throws Exception {
        String xml = "<root><a><b>1</b><c>2</c><b>3</b></a></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();

        assertEquals("a", reader.nextKey());
        assertEquals(StructureReader.Kind.OBJECT, reader.currentKind());
        assertEquals("b", reader.nextKey());
        assertEquals(StructureReader.Kind.ARRAY, reader.currentKind());
        assertEquals("c", reader.nextKey());
        assertEquals(StructureReader.Kind.VALUE, reader.currentKind());
    }

    @Test
    void singleElementLogicalGroupArray() throws Exception {
        String xml = "<root><or><entity><name>x</name></entity></or></root>";
        XmlStructureReader reader = XmlStructureReader.over(xml.getBytes(StandardCharsets.UTF_8));
        reader.begin();

        assertEquals("or", reader.nextKey());
        assertEquals(StructureReader.Kind.ARRAY, reader.currentKind());
        assertEquals(StructureReader.Kind.OBJECT, reader.nextElement());
        assertEquals("name", reader.nextKey());
        assertEquals("x", reader.textValue());
    }
}
