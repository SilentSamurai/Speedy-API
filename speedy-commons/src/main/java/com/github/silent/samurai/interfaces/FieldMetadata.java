package com.github.silent.samurai.interfaces;

import com.github.silent.samurai.enums.IgnoreType;

import java.lang.reflect.InvocationTargetException;

public interface FieldMetadata {

    Object getClassFieldValue(Object entity);

    boolean isAssociation();

    boolean isCollection();

    String getClassFieldName();

    String getDbColumnName();

    String getOutputPropertyName();

    boolean isKeyField();

    IgnoreType getIgnoreType();

    Class<?> getFieldType();

    boolean updateClassFieldWithValue(Object entity, Object value);
}
