package com.github.silent.samurai.speedy.jpa.impl.util;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class JpaUtilTest {

    @Mock
    private EntityType<?> entityType;

    @Mock
    private Type<?> idType;

    // ----------------------------------------
    // Tests for getIdClassType
    // ----------------------------------------
    @Test
    @DisplayName("getIdClassType returns IdClass value when @IdClass is present on entity")
    void testGetIdClassType_WithIdClassAnnotation() {
        doReturn(EntityWithIdClass.class).when(entityType).getBindableJavaType();

        Class<?> resultIdType = JpaUtil.getIdClassType(entityType);
        assertEquals(CompositeId.class, resultIdType);
    }

    @Test
    @DisplayName("getIdClassType falls back to entityType.getIdType().getJavaType() when @IdClass is absent")
    void testGetIdClassType_WithoutIdClassAnnotation() {
        doReturn(SimpleEntity.class).when(entityType).getBindableJavaType();
        doReturn(idType).when(entityType).getIdType();
        doReturn(Long.class).when(idType).getJavaType();

        Class<?> resultIdType = JpaUtil.getIdClassType(entityType);
        assertEquals(Long.class, resultIdType);
    }

    // ----------------------------------------
    // Tests for getTableName
    // ----------------------------------------
    @Test
    @DisplayName("getTableName returns table name when @Entity and @Table are present")
    void testGetTableName_WithEntityAndTable() {
        assertEquals("my_table", JpaUtil.getTableName(EntityWithTable.class));
    }

    @Test
    @DisplayName("getTableName returns null when @Entity is present but @Table is absent")
    void testGetTableName_EntityWithoutTable() {
        assertNull(JpaUtil.getTableName(SimpleEntity.class));
    }

    @Test
    @DisplayName("getTableName returns null when @Entity is absent, even if @Table would be present")
    void testGetTableName_NoEntityAnnotation() {
        assertNull(JpaUtil.getTableName(NoEntityAnnotated.class));
    }

    // ----------------------------------------
    // Tests for getField
    // ----------------------------------------
    @Test
    @DisplayName("getField returns declared field from the same class")
    void testGetField_DirectField() throws Exception {
        Field f = JpaUtil.getField(SubClass.class, "subField");
        assertNotNull(f);
        assertEquals("subField", f.getName());
        assertEquals(SubClass.class, f.getDeclaringClass());
    }

    @Test
    @DisplayName("getField resolves field from superclass when not present in subclass")
    void testGetField_FromSuperclass() throws Exception {
        Field f = JpaUtil.getField(SubClass.class, "baseField");
        assertNotNull(f);
        assertEquals("baseField", f.getName());
        assertEquals(BaseClass.class, f.getDeclaringClass());
    }

    @Test
    @DisplayName("getField throws IllegalStateException when field is not found in class hierarchy")
    void testGetField_NotFound() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JpaUtil.getField(SubClass.class, "missing"));
        assertTrue(ex.getMessage().contains("Could not locate field 'missing'"));
    }

    // ----------------------------------------
    // Tests for resolveGenericFieldType
    // ----------------------------------------
    @Test
    @DisplayName("resolveGenericFieldType returns the first generic type parameter")
    void testResolveGenericFieldType_GenericField() throws Exception {
        Field namesField = GenericHolder.class.getDeclaredField("names");
        Class<?> type = JpaUtil.resolveGenericFieldType(namesField);
        assertEquals(String.class, type);
    }

    @Test
    @DisplayName("resolveGenericFieldType throws RuntimeException when field is not generic")
    void testResolveGenericFieldType_NonGenericField() throws Exception {
        Field titleField = GenericHolder.class.getDeclaredField("title");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> JpaUtil.resolveGenericFieldType(titleField));
        assertEquals("Field is not Generic", ex.getMessage());
    }

    // ----------------------------------------
    // Test fixtures (entities and helper classes)
    // ----------------------------------------
    @Entity
    @Table(name = "my_table")
    static class EntityWithTable {
        @Id
        Long id;
    }

    @Entity
    static class SimpleEntity {
        @Id
        Long id;
    }

    static class NoEntityAnnotated {
    }

    static class CompositeId implements Serializable {
        Long id1;
        Long id2;
    }

    @Entity
    @IdClass(CompositeId.class)
    static class EntityWithIdClass {
        @Id
        Long id1;
        @Id
        Long id2;
    }

    static class BaseClass {
        protected String baseField;
    }

    static class SubClass extends BaseClass {
        private int subField;
    }

    static class GenericHolder {
        List<String> names;
        String title;
    }
}