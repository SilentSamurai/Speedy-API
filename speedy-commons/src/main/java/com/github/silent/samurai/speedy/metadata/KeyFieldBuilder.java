package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;

public class KeyFieldBuilder extends FieldBuilder {
    boolean shouldGenerateKey;

    public KeyFieldBuilder(EntityBuilder entityBuilder, String dbColumnName, ColumnType columnType, String outputPropertyName) {
        super(entityBuilder, dbColumnName, columnType, outputPropertyName);
    }

    public KeyFieldBuilder shouldGenerateKey(boolean shouldGenerateKey) {
        this.shouldGenerateKey = shouldGenerateKey;
        return this;
    }

    @Override
    public KeyFieldMetadataImpl build() {
        return new KeyFieldMetadataImpl(
                super.getColumnType(), super.getDbColumnName(), super.getOutputPropertyName(),
                super.isCollection(), super.isAssociation(), super.isInsertable(), super.isUpdatable(),
                super.isUnique(), super.isNullable(), super.isRequired(), super.isSerializable(),
                super.isDeserializable(), shouldGenerateKey
        );
    }
}
