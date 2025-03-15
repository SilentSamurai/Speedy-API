package com.github.silent.samurai.speedy.data;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.mappings.JavaType2ColumnType;
import jakarta.persistence.*;
import lombok.Data;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Data
public class StaticFieldMetadata implements KeyFieldMetadata {

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
        if (isAssociation()) {
            return getAssociatedFieldMetadata().getColumnType();
        }
        return JavaType2ColumnType.fromClass(field.getType());
    }

    @Override
    public boolean isAssociation() {
        Object[] annotations = {
                field.getAnnotation(OneToMany.class),
                field.getAnnotation(ManyToOne.class),
                field.getAnnotation(ManyToMany.class),
                field.getAnnotation(OneToOne.class)
        };
        Optional<Object> isAnnotationPresent = Arrays.stream(annotations).filter(Objects::nonNull).findAny();
        if (isAnnotationPresent.isPresent()) {
            return true;
        }
        return false;
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

//    @Override
//    public String getClassFieldName() {
//        return field.getName();
//    }

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

//    @SneakyThrows
//    @Override
//    public boolean setIdFieldWithValue(Object idInstance, Object value) {
//        field.set(idInstance, value);
//        return false;
//    }

//    @SneakyThrows
//    @Override
//    public Object getIdFieldValue(Object idInstance) {
//        return field.get(idInstance);
//    }

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
        return StaticEntityMetadata.createEntityMetadata(field.getDeclaringClass());
    }

    @Override
    public EntityMetadata getAssociationMetadata() {
        if (field.getAnnotation(OneToMany.class) != null) {
            return StaticEntityMetadata.createEntityMetadata(field.getType());
        }
        if (field.getAnnotation(OneToOne.class) != null) {
            return StaticEntityMetadata.createEntityMetadata(field.getType());
        }
        return null;
    }

    @Override
    public FieldMetadata getAssociatedFieldMetadata() {
        EntityMetadata associationMetadata = getAssociationMetadata();
        return associationMetadata.getAllFields().stream().filter(fm -> fm.getOutputPropertyName().equals("id")).findAny().orElse(null);
    }

//    @SneakyThrows
//    @Override
//    public boolean setEntityFieldWithValue(Object entity, Object value) {
//        field.set(entity, value);
//        return false;
//    }

//    @SneakyThrows
//    public Object getEntityFieldValue(Object entity) {
//        return field.get(entity);
//    }
}
