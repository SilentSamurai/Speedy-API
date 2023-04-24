package com.github.silent.samurai.deserializer;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.parser.SpeedyUriParser;

import java.util.Optional;

public class ParserIdentityDeserializer {

    private final EntityMetadata entityMetadata;
    private final SpeedyUriParser parser;

    public ParserIdentityDeserializer(SpeedyUriParser parser) {
        this.parser = parser;
        this.entityMetadata = parser.getResourceMetadata();
    }

    public Object deserialize() throws Exception {
        if (entityMetadata.hasCompositeKey()) {
            return this.getCompositeKey();
        }
        return this.getBasicKey();
    }

    private Object getBasicKey() throws Exception {
        Optional<KeyFieldMetadata> primaryKeyFieldMetadata = entityMetadata.getKeyFields().stream().findAny();
        if (primaryKeyFieldMetadata.isPresent()) {
            KeyFieldMetadata keyFieldMetadata = primaryKeyFieldMetadata.get();
            String propertyName = keyFieldMetadata.getOutputPropertyName();
            if (parser.hasKeyword(propertyName)) {
                return parser.getKeyword(propertyName, keyFieldMetadata.getFieldType());
            }
        }
        throw new BadRequestException("primary key field not found");
    }

    private Object getCompositeKey() throws Exception {
        Object newKeyInstance = entityMetadata.createNewKeyInstance();
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            String propertyName = keyFieldMetadata.getOutputPropertyName();
            if (parser.hasKeyword(propertyName)) {
                Object value = parser.getKeyword(propertyName, keyFieldMetadata.getFieldType());
                keyFieldMetadata.setEntityFieldWithValue(newKeyInstance, value);
            } else {
                throw new BadRequestException("primary key incomplete");
            }
        }
        return newKeyInstance;
    }

}
