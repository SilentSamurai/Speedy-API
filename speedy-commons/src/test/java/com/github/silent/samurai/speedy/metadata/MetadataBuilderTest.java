package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MetadataBuilderTest {

    @Test
    void create() throws NotFoundException {

        MetadataBuilder.MetaModelBuilder metaModelBuilder = MetadataBuilder.builder();
        MetadataBuilder.EntityBuilder productBuilder = metaModelBuilder.entity("Product");
        MetadataBuilder.EntityBuilder categoryBuilder = metaModelBuilder.entity("Category");

        categoryBuilder.keyField("id", "ID", ColumnType.UUID)
                .shouldGenerateKey(true);
        categoryBuilder.field("name", "NAME", ColumnType.VARCHAR);

        productBuilder.keyField("id", "ID", ColumnType.UUID);
        productBuilder.field("name", "NAME", ColumnType.VARCHAR);
        productBuilder.field("cid", "CATEGORY_ID", ColumnType.UUID)
                .associateWith(categoryBuilder.ref("id"));

        MetaModel metaModel = metaModelBuilder.build();

        EntityMetadata entityMetadata = metaModel.findEntityMetadata("Product");

        Assertions.assertNotNull(entityMetadata);
        Assertions.assertEquals(entityMetadata.getName(), "Product");
        Assertions.assertEquals(3, entityMetadata.getAllFields().size());

        FieldMetadata id = entityMetadata.field("id");
        Assertions.assertEquals(ColumnType.UUID, id.getColumnType());
        Assertions.assertEquals("ID", id.getDbColumnName());
        Assertions.assertEquals("id", id.getOutputPropertyName());

        FieldMetadata name = entityMetadata.field("name");
        Assertions.assertEquals(ColumnType.VARCHAR, name.getColumnType());
        Assertions.assertEquals("NAME", name.getDbColumnName());
        Assertions.assertEquals("name", name.getOutputPropertyName());

        FieldMetadata category = entityMetadata.field("cid");
        Assertions.assertEquals(ColumnType.UUID, category.getColumnType());
        Assertions.assertEquals("CATEGORY_ID", category.getDbColumnName());
        Assertions.assertEquals("cid", category.getOutputPropertyName());
    }
}