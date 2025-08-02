package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.metadata.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.FieldBuilder;
import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MetadataBuilderTest {

    @Test
    void create() throws NotFoundException {

        MetaModelBuilder metaModelBuilder = MetadataBuilder.builder();
        EntityBuilder productBuilder = metaModelBuilder.entity("Product");
        EntityBuilder categoryBuilder = metaModelBuilder.entity("Category");

        categoryBuilder.keyField("id", "ID", ColumnType.UUID)
                .shouldGenerateKey(true);
        categoryBuilder.field("name", "NAME", ColumnType.VARCHAR);

        productBuilder.keyField("id", "ID", ColumnType.UUID)
                .shouldGenerateKey(true);
        productBuilder.field("name", "NAME", ColumnType.VARCHAR);
        productBuilder.field("price", "PRICE", ColumnType.DOUBLE);
        FieldBuilder categoryField = productBuilder.field("category", "CATEGORY_ID", ColumnType.UUID);
        categoryField.associateWith(categoryBuilder.keyFields().iterator().next());

        MetaModel metaModel = metaModelBuilder.build();
        Assertions.assertEquals(2, metaModel.getAllEntityMetadata().size());

        EntityMetadata product = metaModel.findEntityMetadata("Product");
        Assertions.assertNotNull(product);
        Assertions.assertEquals(4, product.getAllFields().size());

        FieldMetadata category = product.field("category");
        Assertions.assertNotNull(category);
        Assertions.assertTrue(category.isAssociation());

        EntityMetadata categoryEntity = metaModel.findEntityMetadata("Category");
        Assertions.assertNotNull(categoryEntity);
        Assertions.assertEquals(categoryEntity, category.getAssociationMetadata());
    }
}