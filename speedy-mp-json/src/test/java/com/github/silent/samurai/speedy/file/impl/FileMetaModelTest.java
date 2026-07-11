package com.github.silent.samurai.speedy.file.impl;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.enums.BulkOperation;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.file.impl.models.JsonEntity;
import com.github.silent.samurai.speedy.file.impl.models.JsonField;
import com.github.silent.samurai.speedy.file.impl.processor.FileProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileMetaModelTest {

    static FileMetaModelProcessor fileMetaModelProcessor;

    @Mock
    static DataSource dataSource;

    @BeforeAll
    static void setUp() throws IOException {
        fileMetaModelProcessor = new FileMetaModelProcessor("metamodel.json");
        fileMetaModelProcessor.processMetaModel(MetadataBuilder.builder());
    }

    @Test
    void checkCategory() throws NotFoundException {

        MetaModel metaModel = fileMetaModelProcessor.getMetaModel();

        EntityMetadata categoryMetadata = metaModel.findEntityMetadata("Category");

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

        MetaModel metaModel = fileMetaModelProcessor.getMetaModel();

        EntityMetadata productMetadata = metaModel.findEntityMetadata("Product");

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
        assertInstanceOf(KeyFieldMetadata.class, idField);
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
        assertEquals(ValueType.TEXT, categoryField.getValueType());
        assertEquals("category_id", categoryField.getDbColumnName());
        assertEquals("category", categoryField.getOutputPropertyName());

        EntityMetadata categoryMetadata = metaModel.findEntityMetadata("Category");
        assertEquals(categoryMetadata, categoryField.getAssociationMetadata());


    }

    @Test
    void mapsSelectedBulkOperations() throws Exception {
        JsonEntity jsonEntity = jsonEntity("JsonSelectedBulk");
        jsonEntity.bulk = List.of("create", "DELETE");

        MetaModel metaModel = process(jsonEntity);
        EntityMetadata metadata = metaModel.findEntityMetadata("JsonSelectedBulk");

        assertTrue(metadata.isBulkAllowed(BulkOperation.CREATE));
        assertFalse(metadata.isBulkAllowed(BulkOperation.UPDATE));
        assertFalse(metadata.isBulkAllowed(BulkOperation.REPLACE));
        assertTrue(metadata.isBulkAllowed(BulkOperation.DELETE));
    }

    @Test
    @SuppressWarnings("deprecation")
    void mapsLegacyBulkAllowedToAllOperations() throws Exception {
        JsonEntity jsonEntity = jsonEntity("JsonLegacyBulk");
        jsonEntity.bulkAllowed = true;

        EntityMetadata metadata = process(jsonEntity).findEntityMetadata("JsonLegacyBulk");

        for (BulkOperation operation : List.of(
                BulkOperation.CREATE, BulkOperation.UPDATE, BulkOperation.REPLACE, BulkOperation.DELETE)) {
            assertTrue(metadata.isBulkAllowed(operation));
        }
    }

    private MetaModel process(JsonEntity jsonEntity) throws Exception {
        var builder = MetadataBuilder.builder();
        FileProcessor.processEntityMetadata(jsonEntity, builder);
        return builder.build();
    }

    private JsonEntity jsonEntity(String name) {
        JsonField id = new JsonField();
        id.name = "id";
        id.outputProperty = "id";
        id.dbColumn = "id";
        id.fieldType = "VARCHAR";
        id.isKeyField = true;

        JsonEntity entity = new JsonEntity();
        entity.name = name;
        entity.dbTable = name.toLowerCase();
        entity.fields = List.of(id);
        return entity;
    }
}
