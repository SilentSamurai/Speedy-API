package com.github.silent.samurai.interfaces;

import com.github.silent.samurai.enums.IgnoreType;

public interface FieldMetadata {

    Object getEntityFieldValue(Object entity);

    boolean isAssociation();

    boolean isCollection();

    String getClassFieldName();

    String getDbColumnName();

    String getOutputPropertyName();

    IgnoreType getIgnoreType();

    Class<?> getFieldType();

    boolean setEntityFieldWithValue(Object entity, Object value);
}
