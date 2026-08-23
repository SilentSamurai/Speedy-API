package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.EtagStrategy;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.metadata.AssociationColumn;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.models.DynamicEnum;
import com.github.silent.samurai.speedy.validation.rules.FieldRule;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
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
    // Blocked from $ field references when true (from @SpeedySensitive).
    private final boolean isSensitive;
    // Fields to distinguish stored vs operational enum configuration
    private final EnumMode storedEnumMode;
    private final EnumMode operationalEnumMode;
    private final DynamicEnum dynamicEnum;
    private final List<FieldRule> validations;
    private final Optional<EtagStrategy> etagStrategy;

    @Setter
    private EntityMetadata entityMetadata;
    @Setter
    private EntityMetadata associationMetadata;
    /// Every foreign-key column of this association, ordered by the target's key-field order.
    /// Populated by {@link MetaModelBuilder} once every entity's fields are resolvable; empty for a
    /// non-association field.
    private List<AssociationColumn> associationColumns = List.of();

    /// The declared column width; 0 when the field has none. Set after construction rather than
    /// through the constructor, which already carries every other declared property.
    @Setter
    @Getter
    private int maxLength;

    /// The declared digit counts; 0 when the field has none. Set after construction, as maxLength is.
    @Setter
    @Getter
    private int precision;

    @Setter
    @Getter
    private int scale;

    public void setAssociationColumns(List<AssociationColumn> associationColumns) {
        this.associationColumns = associationColumns == null ? List.of() : List.copyOf(associationColumns);
    }

    @Override
    public FieldMetadata getAssociatedFieldMetadata() {
        return associationColumns.isEmpty() ? null : associationColumns.get(0).targetKeyField();
    }

    /// For an association this is the *first foreign-key column*, so that it always describes the
    /// same column {@link #getAssociatedFieldMetadata()} types — the two are read as a pair
    /// throughout (a column and the target key field giving its type), and deriving both from
    /// {@link #getAssociationColumns()} is what keeps them from drifting apart. They would
    /// otherwise: the declared column and the first mapped column are separate inputs, and a
    /// {@code @JoinColumns} list written in a different order than the target's key fields, or a
    /// JSON metamodel naming a different {@code dbColumn} on the field than on its single mapped
    /// column, makes them disagree.
    ///
    /// Falls back to the declared column while the association is still being resolved (which is
    /// when {@link MetaModelBuilder} reads it to fill in an unspecified local column) and for every
    /// non-association field, where it is the only column there is.
    @Override
    public String getDbColumnName() {
        return associationColumns.isEmpty() ? dbColumnName : associationColumns.get(0).localDbColumnName();
    }

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
                             boolean isSensitive,
                             EnumMode storedEnumMode,
                             EnumMode operationalEnumMode,
                             DynamicEnum dynamicEnum,
                             List<FieldRule> validations,
                             Optional<EtagStrategy> etagStrategy) {
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
        this.isSensitive = isSensitive;
        this.validations = validations == null ? List.of() : List.copyOf(validations);
        this.isEnum = isEnum;
        this.etagStrategy = etagStrategy.isEmpty() ? Optional.empty() : etagStrategy;
    }

    /// Deliberately the *declared* column rather than {@link #getDbColumnName()}: identity has to
    /// stay fixed for the object's whole life, and the resolved column only appears once
    /// {@link MetaModelBuilder} sets the association columns — an instance already sitting in a hash
    /// structure would move buckets underneath it.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldMetadataImpl otherMetadata = (FieldMetadataImpl) o;
        return dbColumnName.equals(otherMetadata.dbColumnName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dbColumnName);
    }
}
