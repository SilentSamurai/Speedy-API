package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
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
        assertTrue(verifier.verify());
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
