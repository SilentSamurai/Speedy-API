package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.enums.IgnoreType;
import com.github.silent.samurai.speedy.enums.ValueType;

public interface FieldMetadata {

    ValueType getValueType();

    Object getEntityFieldValue(Object entity);

    boolean isAssociation();

    boolean isCollection();

    boolean isInsertable();

    boolean isUpdatable();

    boolean isUnique();

    boolean isNullable();

    boolean isRequired();

    boolean isSerializable();

    boolean isDeserializable();

    String getClassFieldName();

    String getDbColumnName();

    String getOutputPropertyName();

    IgnoreType getIgnoreProperty();

    Class<?> getFieldType();

    EntityMetadata getEntityMetadata();

    EntityMetadata getAssociationMetadata();

    boolean setEntityFieldWithValue(Object entity, Object value);
}
