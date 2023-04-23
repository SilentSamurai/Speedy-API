package com.github.silent.samurai.interfaces;

import com.github.silent.samurai.enums.IgnoreType;

public interface FieldMetadata {

    Object getEntityFieldValue(Object entity);

    boolean isAssociation();

    boolean isCollection();

    boolean isInsertable();

    boolean isUpdatable();

    boolean isUnique();

    boolean isNullable();

    boolean isSerializable();

    boolean isDeserializable();

    String getClassFieldName();

    String getDbColumnName();

    String getOutputPropertyName();

    IgnoreType getIgnoreType();

    Class<?> getFieldType();

    EntityMetadata getAssociationMetadata();

    boolean setEntityFieldWithValue(Object entity, Object value);
}
