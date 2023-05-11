package com.github.silent.samurai.speedy.deserializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.util.Optional;

public class JsonIdentityDeserializer {

    private final EntityMetadata entityMetadata;
    private final ObjectNode keyJson;

    public JsonIdentityDeserializer(EntityMetadata entityMetadata, ObjectNode keyJson) {
        this.entityMetadata = entityMetadata;
        this.keyJson = keyJson;
    }

    public Object deserialize() throws Exception {
        if (entityMetadata.hasCompositeKey()) {
            return this.getCompositeKey();
        }
        return this.getBasicKey();
    }

    private Object getBasicKey() throws BadRequestException, JsonProcessingException {
        Optional<KeyFieldMetadata> primaryKeyFieldMetadata = this.entityMetadata.getKeyFields().stream().findAny();
        if (primaryKeyFieldMetadata.isPresent()) {
            KeyFieldMetadata keyFieldMetadata = primaryKeyFieldMetadata.get();
            String propertyName = keyFieldMetadata.getOutputPropertyName();
            if (keyJson.has(propertyName)) {
                return CommonUtil.jsonToType(keyJson.get(propertyName), keyFieldMetadata.getFieldType());
            }
        }
        throw new BadRequestException("primary key field not found" + keyJson);
    }

    private Object getCompositeKey() throws Exception {
        Object newKeyInstance = entityMetadata.createNewKeyInstance();
        for (KeyFieldMetadata fieldMetadata : entityMetadata.getKeyFields()) {
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (keyJson.has(propertyName)) {
                Object value = CommonUtil.jsonToType(keyJson.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.setEntityFieldWithValue(newKeyInstance, value);
            } else {
                throw new BadRequestException("primary key incomplete" + keyJson);
            }
        }
        return newKeyInstance;
    }

}
