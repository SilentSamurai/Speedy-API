package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.DynamicEnum;
import com.github.silent.samurai.speedy.validation.rules.FieldRule;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class KeyFieldMetadataImpl extends FieldMetadataImpl implements KeyFieldMetadata {

    private final boolean isKeyField = true;
    private final boolean shouldGenerateKey;

    public KeyFieldMetadataImpl(ColumnType columnType,
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
                                List<FieldRule> validations,
                                boolean shouldGenerateKey) {
        super(columnType,
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
                validations);
        this.shouldGenerateKey = shouldGenerateKey;
    }


    @Override
    public boolean shouldGenerateKey() {
        return shouldGenerateKey;
    }
}
