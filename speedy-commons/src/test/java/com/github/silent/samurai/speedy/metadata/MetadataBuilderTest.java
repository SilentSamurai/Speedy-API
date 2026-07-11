package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
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

    /**
     * Bulk create/delete is disabled by default (no @SpeedyBulk annotation).
     */
    @Test
    void bulkAllowed_defaultsFalse() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("DefaultBulk");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("name", "NAME", ColumnType.VARCHAR);

        EntityMetadata md = entity.build();
        Assertions.assertFalse(md.isBulkAllowed(), "bulk must be disabled by default");
    }

    /**
     * @SpeedyBulk(true) — modelled by EntityBuilder.bulkAllowed(true) — enables bulk.
     */
    @Test
    void bulkAllowed_explicitTrue() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("WithBulk");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("name", "NAME", ColumnType.VARCHAR);
        entity.bulkAllowed(true);

        EntityMetadata md = entity.build();
        Assertions.assertTrue(md.isBulkAllowed(), "bulkAllowed(true) must enable bulk");
    }

    /**
     * Soft delete is off by default (no @SpeedySoftDelete).
     */
    @Test
    void softDelete_defaultsDisabled() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("NoSoftDelete");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("name", "NAME", ColumnType.VARCHAR);

        EntityMetadata md = entity.build();
        Assertions.assertFalse(md.isSoftDeleteEnabled(), "soft delete must be off by default");
        Assertions.assertNull(md.getSoftDeleteField());
        Assertions.assertFalse(md.isViewDeletedAllowed());
        Assertions.assertFalse(md.isHardDeleteAllowed());
    }

    /**
     * A valid temporal marker field enables soft delete and surfaces the gate flags.
     */
    @Test
    void softDelete_temporalField_enabled() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("SoftTemporal");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("deletedAt", "DELETED_AT", ColumnType.TIMESTAMP);
        entity.softDelete("deletedAt", true, true);

        EntityMetadata md = entity.build();
        Assertions.assertTrue(md.isSoftDeleteEnabled());
        Assertions.assertEquals("deletedAt", md.getSoftDeleteField().getOutputPropertyName());
        Assertions.assertTrue(md.isViewDeletedAllowed());
        Assertions.assertTrue(md.isHardDeleteAllowed());
    }

    /**
     * A boolean marker field is also valid; gate flags default to false when not opted in.
     */
    @Test
    void softDelete_booleanField_enabled() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("SoftBool");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("isDeleted", "IS_DELETED", ColumnType.BOOLEAN);
        entity.softDelete("isDeleted", false, false);

        EntityMetadata md = entity.build();
        Assertions.assertTrue(md.isSoftDeleteEnabled());
        Assertions.assertFalse(md.isViewDeletedAllowed());
        Assertions.assertFalse(md.isHardDeleteAllowed());
    }

    /**
     * Naming a marker field that does not exist fails fast at build time.
     */
    @Test
    void softDelete_missingField_throwsNotFound() {
        EntityBuilder entity = MetadataBuilder.builder().entity("SoftMissing");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("name", "NAME", ColumnType.VARCHAR);
        entity.softDelete("deletedAt", false, false);

        Assertions.assertThrows(NotFoundException.class, entity::build);
    }

    /**
     * A marker field of an unsupported type (e.g. text) is rejected at build time.
     */
    @Test
    void softDelete_wrongType_throwsIllegalState() {
        EntityBuilder entity = MetadataBuilder.builder().entity("SoftWrongType");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("deletedAt", "DELETED_AT", ColumnType.VARCHAR);
        entity.softDelete("deletedAt", false, false);

        Assertions.assertThrows(IllegalStateException.class, entity::build);
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