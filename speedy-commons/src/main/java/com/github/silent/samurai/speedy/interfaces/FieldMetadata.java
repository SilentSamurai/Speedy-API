package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.models.DynamicEnum;
import com.github.silent.samurai.speedy.validation.rules.FieldRule;

import java.util.List;

public interface FieldMetadata {

    ColumnType getColumnType();

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

    EntityMetadata getEntityMetadata();

    EntityMetadata getAssociationMetadata();

    FieldMetadata getAssociatedFieldMetadata();

    // Enum metadata
    boolean isEnum();

    // New: explicit accessors for stored vs operational enum config
    EnumMode getStoredEnumMode();

    EnumMode getOperationalEnumMode();

    DynamicEnum getDynamicEnum();

    /**
     * List of generic validation specs derived from field annotations.
     * Implementations may override; default is empty list.
     */
    default List<FieldRule> getValidations() {
        return List.of();
    }

}
