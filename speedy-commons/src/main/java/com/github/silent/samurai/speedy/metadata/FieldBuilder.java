package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.models.DynamicEnum;
import com.github.silent.samurai.speedy.validation.rules.FieldRule;
import lombok.Getter;

import java.util.List;

@Getter
public class FieldBuilder {
    final EntityBuilder entityBuilder;
    final String outputPropertyName;
    ColumnType columnType;
    String dbColumnName;
    ColumnType columnTypeOverride;
    boolean isCollection = false;
    boolean isAssociation = false;
    boolean isInsertable = true;
    boolean isUpdatable = true;
    boolean isUnique = false;
    boolean isNullable = false;
    boolean isRequired = false;
    boolean isSerializable = true;
    boolean isDeserializable = true;
    boolean shouldGenerateKey = false;
    String associatedField;
    String associatedEntity;
    boolean isEnum = false;
    EnumMode storedEnumMode;
    EnumMode operationalEnumMode;
    DynamicEnum dynamicEnum;
    List<FieldRule> validations = new java.util.ArrayList<>();

    public FieldBuilder(EntityBuilder entityBuilder, String name) {
        this.validations = new java.util.ArrayList<>();
        this.entityBuilder = entityBuilder;
        this.dbColumnName = name;
        this.outputPropertyName = name;
    }

    public FieldBuilder dbColumnName(String dbColumnName) {
        this.dbColumnName = dbColumnName;
        return this;
    }

    public FieldBuilder columnType(ColumnType columnType) {
        this.columnType = columnType;
        return this;
    }

    public FieldBuilder columnTypeOverride(ColumnType columnTypeOverride) {
        this.columnTypeOverride = columnTypeOverride;
        return this;
    }

    public FieldBuilder collection(boolean isCollection) {
        this.isCollection = isCollection;
        return this;
    }

    public FieldBuilder insertable(boolean isInsertable) {
        this.isInsertable = isInsertable;
        return this;
    }

    public FieldBuilder updatable(boolean isUpdatable) {
        this.isUpdatable = isUpdatable;
        return this;
    }

    public FieldBuilder unique(boolean isUnique) {
        this.isUnique = isUnique;
        return this;
    }

    public FieldBuilder nullable(boolean isNullable) {
        this.isNullable = isNullable;
        return this;
    }

    public FieldBuilder required(boolean isRequired) {
        this.isRequired = isRequired;
        return this;
    }

    public FieldBuilder serializable(boolean isSerializable) {
        this.isSerializable = isSerializable;
        return this;
    }

    public FieldBuilder deserializable(boolean isDeserializable) {
        this.isDeserializable = isDeserializable;
        return this;
    }

    public FieldBuilder enumField(EnumMode storedEnumMode,
                                  EnumMode operational,
                                  DynamicEnum dynamicEnum) {
        this.isEnum = true;
        this.storedEnumMode = storedEnumMode;
        this.dynamicEnum = dynamicEnum;
        this.operationalEnumMode = operational;
        return this;
    }

    public FieldBuilder associateWith(FieldBuilder associatedField) {
        this.associatedField = associatedField.outputPropertyName;
        this.associatedEntity = associatedField.entityBuilder.getName();
        this.isAssociation = true;
        return this;
    }

    public FieldBuilder associateWith(String entity, String field) {
        this.associatedEntity = entity;
        this.associatedField = field;
        this.isAssociation = true;
        return this;
    }

    public FieldBuilder addValidationRule(FieldRule rule) {
        this.validations.add(rule);
        return this;
    }

    public FieldMetadataImpl build() throws NotFoundException {
        if (!isNullable && isDeserializable) {
            required(true);
        }
        // Compute value type based on override when provided, else use the base columnType
        ValueType valueType = resolveValueType();
        ColumnType columnType = resolveColumnType();
        FieldMetadataImpl fmi = new FieldMetadataImpl(
                columnType,
                valueType,
                dbColumnName,
                outputPropertyName,
                isCollection,
                isAssociation,
                isInsertable,
                isUpdatable,
                isUnique,
                isNullable,
                isRequired,
                isSerializable,
                isDeserializable,
                isEnum,
                storedEnumMode,
                operationalEnumMode,
                dynamicEnum,
                validations
        );
        return fmi;
    }

    public ValueType resolveValueType() throws NotFoundException {
        if (isEnum) {
            return switch (operationalEnumMode) {
                case STRING -> ValueType.ENUM;
                case ORDINAL -> ValueType.ENUM_ORD;
            };
        }
        return resolveColumnType().getValueType();
    }

    public ColumnType resolveColumnType() throws NotFoundException {
        // 1) explicit override wins
        if (columnTypeOverride != null) {
            return columnTypeOverride;
        }

        // 2) enum handling if applicable
        if (isEnum) {
            return switch (storedEnumMode) {
                case STRING -> ColumnType.VARCHAR;
                case ORDINAL -> ColumnType.INTEGER;
            };
        }

        if (isAssociation) {
            return ColumnType.VARCHAR;
        }

        if (columnType != null) {
            return columnType;
        }

        throw new NotFoundException("Unable to resolve column type for " + entityBuilder.getName() + "." + dbColumnName);
    }

    // Backward compatibility for callers expecting getEnumMode() on builder
    public EnumMode getEnumMode() {
        return operationalEnumMode;
    }
}
