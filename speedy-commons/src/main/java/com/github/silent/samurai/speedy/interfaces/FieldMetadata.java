package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.ValueType;

public interface FieldMetadata {

    ValueType getValueType();

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

    ActionType getIgnoreProperty();

    Class<?> getFieldType();

    EntityMetadata getEntityMetadata();

    EntityMetadata getAssociationMetadata();

    FieldMetadata getAssociatedFieldMetadata();

//    // foreign_key -> primary_key
//    // entity_field -> associated_key
//    Map<String, String> getAssociatedFields();
}
