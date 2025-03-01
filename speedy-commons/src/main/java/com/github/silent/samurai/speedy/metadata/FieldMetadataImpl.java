package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class FieldMetadataImpl implements FieldMetadata {
    private final ColumnType columnType;
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
    private EntityMetadata entityMetadata;
    private EntityMetadata associationMetadata;
    private FieldMetadata associatedFieldMetadata;

    public FieldMetadataImpl(ColumnType columnType, String dbColumnName, String outputPropertyName, boolean isCollection, boolean isAssociation, boolean isInsertable, boolean isUpdatable, boolean isUnique, boolean isNullable, boolean isRequired, boolean isSerializable, boolean isDeserializable) {
        this.columnType = columnType;
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
