package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;
import lombok.Getter;

@Getter
public class FieldBuilder {
    final EntityBuilder entityBuilder;
    final ColumnType columnType;
    final String dbColumnName;
    final String outputPropertyName;
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

    public FieldBuilder(EntityBuilder entityBuilder, String dbColumnName, ColumnType columnType, String outputPropertyName) {
        this.entityBuilder = entityBuilder;
        this.columnType = columnType;
        this.dbColumnName = dbColumnName;
        this.outputPropertyName = outputPropertyName;
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

    public FieldMetadataImpl build() {
        if (!isNullable && isDeserializable) {
            required(true);
        }
        return new FieldMetadataImpl(
                columnType, dbColumnName, outputPropertyName,
                isCollection, isAssociation, isInsertable, isUpdatable, isUnique,
                isNullable, isRequired, isSerializable, isDeserializable
        );
    }
}
