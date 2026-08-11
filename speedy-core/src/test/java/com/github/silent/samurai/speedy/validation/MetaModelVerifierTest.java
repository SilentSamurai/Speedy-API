package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.metadata.AssociationColumnRef;
import com.github.silent.samurai.speedy.metadata.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaModelVerifierTest {

    @Mock
    private MetaModel metaModel;

    @Mock
    private EntityMetadata entityMetadata;

    @Mock
    private FieldMetadata fieldMetadata;

    @Test
    void testVerifyHappyPath() throws Exception {
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(entityMetadata));
        when(entityMetadata.getName()).thenReturn("TestEntity");
        when(entityMetadata.getDbTableName()).thenReturn("test_entity");
        when(entityMetadata.getAllFields()).thenReturn(Set.of(fieldMetadata));
        when(fieldMetadata.isAssociation()).thenReturn(false);
        when(fieldMetadata.getValueType()).thenReturn(ValueType.TEXT);
        when(fieldMetadata.getDbColumnName()).thenReturn("test_column");

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        assertDoesNotThrow(verifier::verify);
    }

    @Test
    void testNullEntityMetadata() {
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(null));

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        assertThrows(NullPointerException.class, verifier::verify);
    }

    @Test
    void testNullEntityName() {
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(entityMetadata));
        when(entityMetadata.getName()).thenReturn(null);

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        NullPointerException ex = assertThrows(NullPointerException.class, verifier::verify);
        assertEquals("Entity Name not found", ex.getMessage());
    }

    @Test
    void testNullDbTableName() {
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(entityMetadata));
        when(entityMetadata.getName()).thenReturn("TestEntity");
        when(entityMetadata.getDbTableName()).thenReturn(null);

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        NullPointerException ex = assertThrows(NullPointerException.class, verifier::verify);
        assertEquals("TestEntity Db Table Name not found", ex.getMessage());
    }

    @Test
    void testNullFieldMetadata() {
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(entityMetadata));
        when(entityMetadata.getName()).thenReturn("TestEntity");
        when(entityMetadata.getDbTableName()).thenReturn("test_entity");
        when(entityMetadata.getAllFields()).thenReturn(Collections.singleton(null));

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        assertThrows(NullPointerException.class, verifier::verify);
    }

    @Test
    void testNullValueType() {
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(entityMetadata));
        when(entityMetadata.getName()).thenReturn("TestEntity");
        when(entityMetadata.getDbTableName()).thenReturn("test_entity");
        when(entityMetadata.getAllFields()).thenReturn(Set.of(fieldMetadata));
        when(fieldMetadata.getValueType()).thenReturn(null);

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        assertThrows(NullPointerException.class, verifier::verify);
    }

    @Test
    void testNullDbColumnName() {
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(entityMetadata));
        when(entityMetadata.getName()).thenReturn("TestEntity");
        when(entityMetadata.getDbTableName()).thenReturn("test_entity");
        when(entityMetadata.getAllFields()).thenReturn(Set.of(fieldMetadata));
        when(fieldMetadata.getValueType()).thenReturn(ValueType.TEXT);
        when(fieldMetadata.getDbColumnName()).thenReturn(null);

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        NullPointerException ex = assertThrows(NullPointerException.class, verifier::verify);
        assertEquals("TestEntity Db Column Name not found", ex.getMessage());
    }

    @Test
    void testNullAssociationMetadata() {
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(entityMetadata));
        when(entityMetadata.getName()).thenReturn("TestEntity");
        when(entityMetadata.getDbTableName()).thenReturn("test_entity");
        when(entityMetadata.getAllFields()).thenReturn(Set.of(fieldMetadata));
        when(fieldMetadata.getValueType()).thenReturn(ValueType.TEXT);
        when(fieldMetadata.getDbColumnName()).thenReturn("test_column");
        when(fieldMetadata.isAssociation()).thenReturn(true);
        when(fieldMetadata.getAssociationMetadata()).thenReturn(null);

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        assertThrows(NullPointerException.class, verifier::verify);
    }

    @Test
    void testNullAssociatedFieldMetadata() {
        EntityMetadata associatedMetadata = org.mockito.Mockito.mock(EntityMetadata.class);
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(entityMetadata));
        when(entityMetadata.getName()).thenReturn("TestEntity");
        when(entityMetadata.getDbTableName()).thenReturn("test_entity");
        when(entityMetadata.getAllFields()).thenReturn(Set.of(fieldMetadata));
        when(fieldMetadata.getValueType()).thenReturn(ValueType.TEXT);
        when(fieldMetadata.getDbColumnName()).thenReturn("test_column");
        when(fieldMetadata.isAssociation()).thenReturn(true);
        when(fieldMetadata.getAssociationMetadata()).thenReturn(associatedMetadata);
        when(fieldMetadata.getAssociatedFieldMetadata()).thenReturn(null);

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        assertThrows(NullPointerException.class, verifier::verify);
    }

    @Test
    void testObjectTypeWithoutAssociation() {
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(entityMetadata));
        when(entityMetadata.getName()).thenReturn("TestEntity");
        when(entityMetadata.getDbTableName()).thenReturn("test_entity");
        when(entityMetadata.getAllFields()).thenReturn(Set.of(fieldMetadata));
        when(fieldMetadata.getValueType()).thenReturn(ValueType.OBJECT);
        when(fieldMetadata.getDbColumnName()).thenReturn("test_column");
        when(fieldMetadata.isAssociation()).thenReturn(false);
        when(fieldMetadata.getOutputPropertyName()).thenReturn("testField");

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        InternalServerError ex = assertThrows(InternalServerError.class, verifier::verify);
        assertEquals(
                "field testField in entity TestEntity is derived as speedy object type which is not supported",
                ex.getMessage());
    }

    /// A metamodel built outside the JPA processor can name a single foreign-key column for a target
    /// whose key spans several — the annotation check that would have caught it never runs. Left
    /// unchecked, reads populate a partial key and resolve to an arbitrary sibling row.
    @Test
    void partiallyMappedCompositeKeyAssociation_isRejected() throws Exception {
        MetaModelBuilder builder = MetadataBuilder.builder();
        compositeKeyTarget(builder);
        EntityBuilder shipment = builder.entity("OrderShipment").dbTableName("order_shipments");
        shipment.keyField("id", "id", ColumnType.VARCHAR);
        shipment.field("order", "order_product_id", ColumnType.VARCHAR)
                .associateWith("Order", "productId");

        MetaModelVerifier verifier = new MetaModelVerifier(builder.build());
        InternalServerError ex = assertThrows(InternalServerError.class, verifier::verify);
        assertTrue(ex.getMessage().contains("must be mapped through every key column of its target"),
                ex.getMessage());
    }

    @Test
    void fullyMappedCompositeKeyAssociation_isAccepted() throws Exception {
        MetaModelBuilder builder = MetadataBuilder.builder();
        compositeKeyTarget(builder);
        EntityBuilder shipment = builder.entity("OrderShipment").dbTableName("order_shipments");
        shipment.keyField("id", "id", ColumnType.VARCHAR);
        shipment.field("order", "order_product_id", ColumnType.VARCHAR)
                .associateWith("Order", List.of(
                        new AssociationColumnRef("order_product_id", "productId"),
                        new AssociationColumnRef("order_supplier_id", "supplierId")));

        MetaModelVerifier verifier = new MetaModelVerifier(builder.build());
        assertDoesNotThrow(verifier::verify);
    }

    private static void compositeKeyTarget(MetaModelBuilder builder) {
        EntityBuilder order = builder.entity("Order").dbTableName("orders").hasCompositeKey(true);
        order.keyField("productId", "product_id", ColumnType.VARCHAR);
        order.keyField("supplierId", "supplier_id", ColumnType.VARCHAR);
        order.field("price", "price", ColumnType.DOUBLE);
    }

    @Test
    void testCollectionTypeWithoutAssociation() {
        when(metaModel.getAllEntityMetadata()).thenReturn(Collections.singletonList(entityMetadata));
        when(entityMetadata.getName()).thenReturn("TestEntity");
        when(entityMetadata.getDbTableName()).thenReturn("test_entity");
        when(entityMetadata.getAllFields()).thenReturn(Set.of(fieldMetadata));
        when(fieldMetadata.getValueType()).thenReturn(ValueType.COLLECTION);
        when(fieldMetadata.getDbColumnName()).thenReturn("test_column");
        when(fieldMetadata.isAssociation()).thenReturn(false);
        when(fieldMetadata.getOutputPropertyName()).thenReturn("testField");

        MetaModelVerifier verifier = new MetaModelVerifier(metaModel);
        InternalServerError ex = assertThrows(InternalServerError.class, verifier::verify);
        assertEquals(
                "field testField in entity TestEntity is derived as speedy object type which is not supported",
                ex.getMessage());
    }
}
