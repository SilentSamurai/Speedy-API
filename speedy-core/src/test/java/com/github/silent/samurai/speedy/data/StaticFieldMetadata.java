package com.github.silent.samurai.speedy.data;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.utils.ValueTypeUtil;
import lombok.Data;

import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;

@Data
public class StaticFieldMetadata implements KeyFieldMetadata {

    private Field field;

    public static KeyFieldMetadata createFieldMetadata(Field field) {
        StaticFieldMetadata fieldMetadata = new StaticFieldMetadata();
        field.setAccessible(true);
        fieldMetadata.setField(field);
        return fieldMetadata;
    }

    @Override
    public ValueType getValueType() {
        return ValueTypeUtil.fromClass(field.getType());
    }

//    @SneakyThrows
//    @Override
//    public Object getEntityFieldValue(Object entity) {
//        return field.get(entity);
//    }

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

    @Override
    public ActionType getIgnoreProperty() {
        return ActionType.ALL;
    }

    @Override
    public Class<?> getFieldType() {
        return field.getType();
    }

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
        return associationMetadata.getAllFields()
                .stream()
                .filter(fm -> fm.getOutputPropertyName().equals("id"))
                .findAny().orElse(null);
    }

//    @SneakyThrows
//    @Override
//    public boolean setEntityFieldWithValue(Object entity, Object value) {
//        field.set(entity, value);
//        return false;
//    }
}
