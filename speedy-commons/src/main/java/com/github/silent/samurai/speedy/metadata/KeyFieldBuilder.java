package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;

public class KeyFieldBuilder extends FieldBuilder {
    boolean shouldGenerateKey = false;

    public KeyFieldBuilder(EntityBuilder entityBuilder, String name) {
        super(entityBuilder, name);
    }

    public KeyFieldBuilder shouldGenerateKey(boolean shouldGenerateKey) {
        this.shouldGenerateKey = shouldGenerateKey;
        return this;
    }

    @Override
    public KeyFieldMetadataImpl build() throws NotFoundException {
        FieldMetadataImpl fmi = super.build();
        KeyFieldMetadataImpl kfmi = new KeyFieldMetadataImpl(
                fmi.getColumnType(),
                fmi.getValueType(),
                fmi.getDbColumnName(),
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
                fmi.getDynamicEnum(),
                fmi.getValidations(),
                shouldGenerateKey
        );
        return kfmi;
    }
}
