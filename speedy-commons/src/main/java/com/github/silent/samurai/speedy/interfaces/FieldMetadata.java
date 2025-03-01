package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.ValueType;

public interface FieldMetadata {

    ColumnType getColumnType();

    default ValueType getValueType() {
        return getColumnType().getValueType();
    }

    boolean isAssociation();

    boolean isCollection();

    boolean isInsertable();

    boolean isUpdatable();

    boolean isUnique();

    boolean isNullable();

    boolean isRequired();

    boolean isSerializable();

    boolean isDeserializable();

    String getDbColumnName();

    String getOutputPropertyName();

    EntityMetadata getEntityMetadata();

    EntityMetadata getAssociationMetadata();

    FieldMetadata getAssociatedFieldMetadata();


}
