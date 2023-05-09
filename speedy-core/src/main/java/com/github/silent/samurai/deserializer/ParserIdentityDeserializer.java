package com.github.silent.samurai.deserializer;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.models.conditions.BinarySVCondition;
import com.github.silent.samurai.parser.ResourceSelector;

import java.util.Optional;

public class ParserIdentityDeserializer {

    private final EntityMetadata entityMetadata;
    private final ResourceSelector resourceSelector;

    public ParserIdentityDeserializer(ResourceSelector resourceSelector) {
        this.resourceSelector = resourceSelector;
        this.entityMetadata = resourceSelector.getResourceMetadata();
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
            if (resourceSelector.hasKeyword(propertyName)) {
                BinarySVCondition condition = resourceSelector.getFirstConditionByField(propertyName);
                return condition.getInstance();
            }
        }
        throw new BadRequestException("primary key field not found");
    }

    private Object getCompositeKey() throws Exception {
        Object newKeyInstance = entityMetadata.createNewKeyInstance();
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            String propertyName = keyFieldMetadata.getOutputPropertyName();
            if (resourceSelector.hasKeyword(propertyName)) {
                BinarySVCondition condition = resourceSelector.getFirstConditionByField(propertyName);
                Object instance = condition.getInstance();
                keyFieldMetadata.setEntityFieldWithValue(newKeyInstance, instance);
            } else {
                throw new BadRequestException("primary key incomplete");
            }
        }
        return newKeyInstance;
    }

}
