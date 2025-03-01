package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KeyFieldMetadataImpl extends FieldMetadataImpl implements KeyFieldMetadata {

    private boolean isKeyField = true;
    private boolean shouldGenerateKey = false;

    public KeyFieldMetadataImpl(ColumnType columnType, String dbColumnName, String outputPropertyName, boolean isCollection, boolean isAssociation, boolean isInsertable, boolean isUpdatable, boolean isUnique, boolean isNullable, boolean isRequired, boolean isSerializable, boolean isDeserializable, boolean shouldGenerateKey) {
        super(columnType, dbColumnName, outputPropertyName, isCollection, isAssociation, isInsertable, isUpdatable, isUnique, isNullable, isRequired, isSerializable, isDeserializable);
    }

    @Override
    public boolean shouldGenerateKey() {
        return shouldGenerateKey;
    }
}
