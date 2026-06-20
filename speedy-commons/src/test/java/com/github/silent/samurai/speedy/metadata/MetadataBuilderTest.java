package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MetadataBuilderTest {

    /**
     * A specific @SpeedyAction (e.g. READ) must drop the implicit ALL so that
     * writes are actually denied. This is DB-independent on purpose: it catches
     * the gate regression on every backend, including H2, instead of relying on
     * a DB insert happening to fail.
     */
    @Test
    void readOnlyAction_blocksWrites() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("ReadOnly");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("name", "NAME", ColumnType.VARCHAR);
        entity.addActionType(ActionType.READ);

        EntityMetadata md = entity.build();
        Assertions.assertTrue(md.isReadAllowed(), "READ must stay allowed");
        Assertions.assertFalse(md.isCreateAllowed(), "@SpeedyAction(READ) must deny create");
        Assertions.assertFalse(md.isUpdateAllowed(), "@SpeedyAction(READ) must deny update");
        Assertions.assertFalse(md.isDeleteAllowed(), "@SpeedyAction(READ) must deny delete");
    }

    /**
     * No @SpeedyAction means the entity defaults to ALL — every operation allowed.
     */
    @Test
    void noAction_allowsEverything() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("Open");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("name", "NAME", ColumnType.VARCHAR);

        EntityMetadata md = entity.build();
        Assertions.assertTrue(md.isReadAllowed());
        Assertions.assertTrue(md.isCreateAllowed());
        Assertions.assertTrue(md.isUpdateAllowed());
        Assertions.assertTrue(md.isDeleteAllowed());
    }

    /**
     * Granting a subset enables exactly those operations and nothing else.
     */
    @Test
    void readCreateAction_allowsOnlyThose() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("ReadCreate");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("name", "NAME", ColumnType.VARCHAR);
        entity.addActionType(ActionType.READ);
        entity.addActionType(ActionType.CREATE);

        EntityMetadata md = entity.build();
        Assertions.assertTrue(md.isReadAllowed());
        Assertions.assertTrue(md.isCreateAllowed());
        Assertions.assertFalse(md.isUpdateAllowed());
        Assertions.assertFalse(md.isDeleteAllowed());
    }

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