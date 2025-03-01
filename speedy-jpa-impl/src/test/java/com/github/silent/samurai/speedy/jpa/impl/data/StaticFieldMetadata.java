package com.github.silent.samurai.speedy.jpa.impl.data;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaKeyFieldMetadata;
import com.github.silent.samurai.speedy.mappings.JavaType2ColumnType;
import lombok.Data;
import lombok.SneakyThrows;

import jakarta.persistence.Id;

import java.lang.reflect.Field;

@Data
public class StaticFieldMetadata implements IJpaKeyFieldMetadata {

    private Field field;

    public static KeyFieldMetadata createFieldMetadata(Field field) {
        StaticFieldMetadata fieldMetadata = new StaticFieldMetadata();
        field.setAccessible(true);
        fieldMetadata.setField(field);
        return fieldMetadata;
    }

    @SneakyThrows
    @Override
    public ColumnType getColumnType() {
        return JavaType2ColumnType.fromClass(field.getType());
    }

    @SneakyThrows
    @Override
    public Object getEntityFieldValue(Object entity) {
        return field.get(entity);
    }

    @Override
    public boolean isAssociation() {
        return field.getType().isAssignableFrom(ProductItem.class);
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public boolean isInsertable() {
        return true;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean isSerializable() {
        return true;
    }

    @Override
    public boolean isDeserializable() {
        return true;
    }

    @Override
    public String getClassFieldName() {
        return field.getName();
    }

    @Override
    public String getDbColumnName() {
        return field.getName();
    }

    @Override
    public String getOutputPropertyName() {
        return field.getName();
    }

    @Override
    public boolean isKeyField() {
        return field.getAnnotation(Id.class) != null;
    }

    @Override
    public boolean shouldGenerateKey() {
        return true;
    }

    @SneakyThrows
    @Override
    public boolean setIdFieldWithValue(Object idInstance, Object value) {
        field.set(idInstance, value);
        return false;
    }

    @SneakyThrows
    @Override
    public Object getIdFieldValue(Object idInstance) {
        return field.get(idInstance);
    }

//    @Override
//    public ActionType getIgnoreProperty() {
//        return ActionType.ALL;
//    }
//
//    @Override
//    public Class<?> getFieldType() {
//        return field.getType();
//    }

    @Override
    public EntityMetadata getEntityMetadata() {
        return null;
    }

    @Override
    public EntityMetadata getAssociationMetadata() {
        return StaticEntityMetadata.createEntityMetadata(ProductItem.class);
    }

    @Override
    public FieldMetadata getAssociatedFieldMetadata() {
        return null;
    }

    @SneakyThrows
    @Override
    public boolean setEntityFieldWithValue(Object entity, Object value) {
        field.set(entity, value);
        return false;
    }
}
