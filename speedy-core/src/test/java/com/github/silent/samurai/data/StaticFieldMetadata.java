package com.github.silent.samurai.data;

import com.github.silent.samurai.enums.IgnoreType;
import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import lombok.Data;
import lombok.SneakyThrows;

import javax.persistence.Id;
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

    @SneakyThrows
    @Override
    public Object getEntityFieldValue(Object entity) {
        return field.get(entity);
    }

    @Override
    public boolean isAssociation() {
        return false;
    }

    @Override
    public boolean isCollection() {
        return false;
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
    public IgnoreType getIgnoreType() {
        return IgnoreType.ALL;
    }

    @Override
    public Class<?> getFieldType() {
        return field.getType();
    }

    @SneakyThrows
    @Override
    public boolean setEntityFieldWithValue(Object entity, Object value) {
        field.set(entity, value);
        return false;
    }
}
