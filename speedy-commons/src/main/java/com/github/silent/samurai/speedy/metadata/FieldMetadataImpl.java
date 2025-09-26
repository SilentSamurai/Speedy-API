package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.models.DynamicEnum;
import com.github.silent.samurai.speedy.validation.rules.FieldRule;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class FieldMetadataImpl implements FieldMetadata {
    private final ColumnType columnType;
    private final ValueType valueType;
    private final String dbColumnName;
    private final String outputPropertyName;
    private final boolean isCollection;
    private final boolean isAssociation;
    private final boolean isInsertable;
    private final boolean isUpdatable;
    private final boolean isUnique;
    private final boolean isNullable;
    private final boolean isRequired;
    private final boolean isSerializable;
    private final boolean isDeserializable;
    private final boolean isEnum;
    // Fields to distinguish stored vs operational enum configuration
    private final EnumMode storedEnumMode;
    private final EnumMode operationalEnumMode;
    private final DynamicEnum dynamicEnum;
    private final List<FieldRule> validations;

    private EntityMetadata entityMetadata;
    private EntityMetadata associationMetadata;
    private FieldMetadata associatedFieldMetadata;

    public FieldMetadataImpl(ColumnType columnType,
                             ValueType valueType,
                             String dbColumnName,
                             String outputPropertyName,
                             boolean isCollection,
                             boolean isAssociation,
                             boolean isInsertable,
                             boolean isUpdatable,
                             boolean isUnique,
                             boolean isNullable,
                             boolean isRequired,
                             boolean isSerializable,
                             boolean isDeserializable,
                             boolean isEnum,
                             EnumMode storedEnumMode,
                             EnumMode operationalEnumMode,
                             DynamicEnum dynamicEnum,
                             List<FieldRule> validations) {
        this.columnType = columnType;
        this.valueType = valueType;
        this.dbColumnName = dbColumnName;
        this.outputPropertyName = outputPropertyName;
        this.isCollection = isCollection;
        this.isAssociation = isAssociation;
        this.isInsertable = isInsertable;
        this.isUpdatable = isUpdatable;
        this.isUnique = isUnique;
        this.isNullable = isNullable;
        this.isRequired = isRequired;
        this.isSerializable = isSerializable;
        this.isDeserializable = isDeserializable;
        this.storedEnumMode = storedEnumMode;
        this.operationalEnumMode = operationalEnumMode;
        this.dynamicEnum = dynamicEnum;
        this.validations = validations == null ? List.of() : List.copyOf(validations);
        this.isEnum = isEnum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldMetadata otherMetadata = (FieldMetadata) o;
        return dbColumnName.equals(otherMetadata.getDbColumnName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dbColumnName);
    }
}
