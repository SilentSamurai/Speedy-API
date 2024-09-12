package com.github.silent.samurai.speedy.file.impl;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileMetaModelProcessorTest {

    static FileMetaModelProcessor fileMetaModelProcessor;

    @BeforeAll
    static void setUp() throws IOException {
        fileMetaModelProcessor = new FileMetaModelProcessor("metamodel.json");
    }

    @Test
    void checkCategory() throws IOException, NotFoundException {

        EntityMetadata categoryMetadata = fileMetaModelProcessor.findEntityMetadata("Category");

        assertNotNull(categoryMetadata);
        assertEquals("Category", categoryMetadata.getName());

        Set<String> allFieldNames = categoryMetadata.getAllFieldNames();
        assertNotNull(allFieldNames);
        assertEquals(2, allFieldNames.size());
        assertTrue(allFieldNames.contains("id"));
        assertTrue(allFieldNames.contains("name"));

        FieldMetadata idField = categoryMetadata.field("id");
        assertNotNull(idField);
        assertTrue(idField instanceof KeyFieldMetadata);
        assertEquals(ValueType.TEXT, idField.getValueType());
        assertEquals("id", idField.getDbColumnName());
        assertEquals("id", idField.getOutputPropertyName());


        FieldMetadata nameField = categoryMetadata.field("name");
        assertNotNull(nameField);
        assertEquals(ValueType.TEXT, nameField.getValueType());
        assertEquals("name", nameField.getDbColumnName());
        assertEquals("name", nameField.getOutputPropertyName());

    }

    @Test
    void checkProduct() throws NotFoundException {

        EntityMetadata productMetadata = fileMetaModelProcessor.findEntityMetadata("Product");

        assertNotNull(productMetadata);
        assertEquals("Product", productMetadata.getName());

        Set<String> allFieldNames = productMetadata.getAllFieldNames();
        assertNotNull(allFieldNames);
        assertEquals(4, allFieldNames.size());
        assertTrue(allFieldNames.contains("id"));
        assertTrue(allFieldNames.contains("name"));
        assertTrue(allFieldNames.contains("description"));
        assertTrue(allFieldNames.contains("category"));

        FieldMetadata idField = productMetadata.field("id");
        assertNotNull(idField);
        assertTrue(idField instanceof KeyFieldMetadata);
        assertEquals(ValueType.TEXT, idField.getValueType());
        assertEquals("id", idField.getDbColumnName());
        assertEquals("id", idField.getOutputPropertyName());


        FieldMetadata nameField = productMetadata.field("name");
        assertNotNull(nameField);
        assertEquals(ValueType.TEXT, nameField.getValueType());
        assertEquals("name", nameField.getDbColumnName());
        assertEquals("name", nameField.getOutputPropertyName());


        FieldMetadata categoryField = productMetadata.field("category");
        assertNotNull(categoryField);
        assertTrue(categoryField.isAssociation());
        assertEquals(ValueType.OBJECT, categoryField.getValueType());
        assertEquals("category_id", categoryField.getDbColumnName());
        assertEquals("category", categoryField.getOutputPropertyName());

        EntityMetadata categoryMetadata = fileMetaModelProcessor.findEntityMetadata("Category");
        assertEquals(categoryMetadata, categoryField.getAssociationMetadata());


    }
}