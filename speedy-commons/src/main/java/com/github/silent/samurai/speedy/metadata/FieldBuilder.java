package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.EtagStrategy;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.models.DynamicEnum;
import com.github.silent.samurai.speedy.validation.rules.FieldRule;
import lombok.Getter;

import java.util.List;

import static java.util.Optional.ofNullable;

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
    List<AssociationColumnRef> associationColumns = List.of();
    boolean isEnum = false;
    // Inherited from the entity's @SpeedySensitive or set directly via
    // field-level @SpeedySensitive. Determines whether this field is
    // blocked from $ field references in query expressions.
    boolean isSensitive = false;
    EnumMode storedEnumMode;
    EnumMode operationalEnumMode;
    DynamicEnum dynamicEnum;
    List<FieldRule> validations = new java.util.ArrayList<>();
    /// The declared column width, 0 for none. See {@link FieldMetadata#getMaxLength()}.
    int maxLength = 0;
    EtagStrategy etagStrategy;

    public FieldBuilder(EntityBuilder entityBuilder, String name) {
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
        return associateWith(associatedField.entityBuilder.getName(), associatedField.outputPropertyName);
    }

    public FieldBuilder associateWith(String entity, String field) {
        // Null local column: this field's own column carries the foreign key, resolved at build time
        // so a later dbColumnName(...) call still wins.
        return associateWith(entity, List.of(new AssociationColumnRef(null, field)));
    }

    /// Declares a foreign key spanning {@code columns} — one entry per column of the target's
    /// primary key, in the target's key-field order. Use the single-column overloads unless the
    /// target has a composite key.
    public FieldBuilder associateWith(String entity, List<AssociationColumnRef> columns) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "association %s.%s must be mapped through at least one column",
                    entityBuilder.getName(), outputPropertyName));
        }
        this.associatedEntity = entity;
        this.associatedField = columns.get(0).targetFieldName();
        this.associationColumns = List.copyOf(columns);
        this.isAssociation = true;
        return this;
    }

    public FieldBuilder maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public FieldBuilder addValidationRule(FieldRule rule) {
        this.validations.add(rule);
        return this;
    }

    public FieldBuilder sensitive(boolean isSensitive) {
        this.isSensitive = isSensitive;
        return this;
    }

    /// Marks this field as the entity's Speedy-managed conditional-request token (from
    /// {@code @SpeedyETag} or a compatible {@code @Version}). Speedy stamps a fresh value into it
    /// directly on every create/update/replace (bypassing insertable/updatable, the same way a
    /// {@code @Generated}/{@code @Formula} column is computed rather than client-supplied) using
    /// {@code strategy}, so callers should also mark the field not deserializable — a
    /// client-supplied value must never be trusted — and typically not insertable/updatable
    /// either, so it's correctly excluded from generated create/update request schemas.
    public FieldBuilder etagField(EtagStrategy strategy) {
        this.etagStrategy = strategy;
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
                isSensitive,
                storedEnumMode,
                operationalEnumMode,
                dynamicEnum,
                validations,
                ofNullable(etagStrategy)
        );
        fmi.setMaxLength(maxLength);
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
