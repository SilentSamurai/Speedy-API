package com.github.silent.samurai.speedy.deserializer;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;

import java.util.Optional;

public class ParserIdentityDeserializer {

    private final EntityMetadata entityMetadata;
    private final SpeedyQuery speedyQuery;

    public ParserIdentityDeserializer(SpeedyQuery speedyQuery) {
        this.speedyQuery = speedyQuery;
        this.entityMetadata = speedyQuery.getFrom();
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
            // TODO:
//            if (resourceSelector.hasKeyword(propertyName)) {
//                BinarySVCondition condition = resourceSelector.getFirstConditionByField(propertyName);
//                return condition.getInstance();
//            }
        }
        throw new BadRequestException("primary key field not found");
    }

    private Object getCompositeKey() throws Exception {
        Object newKeyInstance = entityMetadata.createNewKeyInstance();
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            String propertyName = keyFieldMetadata.getOutputPropertyName();
            // TODO:
//            if (resourceSelector.hasKeyword(propertyName)) {
//                BinarySVCondition condition = resourceSelector.getFirstConditionByField(propertyName);
//                Object instance = condition.getInstance();
//                keyFieldMetadata.setIdFieldWithValue(newKeyInstance, instance);
//            } else {
//                throw new BadRequestException(String.format("primary key incomplete, field not found '%s' ", propertyName));
//            }
        }
        return newKeyInstance;
    }

}
