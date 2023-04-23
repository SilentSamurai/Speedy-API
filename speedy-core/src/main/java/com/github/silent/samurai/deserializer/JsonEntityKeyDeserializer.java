package com.github.silent.samurai.deserializer;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.JsonObject;

import java.util.Optional;

public class JsonEntityKeyDeserializer {

    private final EntityMetadata entityMetadata;
    private final JsonObject keyJson;

    public JsonEntityKeyDeserializer(EntityMetadata entityMetadata, JsonObject keyJson) {
        this.entityMetadata = entityMetadata;
        this.keyJson = keyJson;
    }

    public Object deserialize() throws Exception {
        if (entityMetadata.hasCompositeKey()) {
            return this.getCompositeKey();
        }
        return this.getBasicKey();
    }

    private Object getBasicKey() throws BadRequestException {
        Optional<KeyFieldMetadata> primaryKeyFieldMetadata = this.entityMetadata.getKeyFields().stream().findAny();
        if (primaryKeyFieldMetadata.isPresent()) {
            KeyFieldMetadata keyFieldMetadata = primaryKeyFieldMetadata.get();
            String propertyName = keyFieldMetadata.getOutputPropertyName();
            if (keyJson.has(propertyName)) {
                return CommonUtil.gsonToType(keyJson.get(propertyName), keyFieldMetadata.getFieldType());
            }
        }
        throw new BadRequestException("primary key field not found" + keyJson);
    }

    private Object getCompositeKey() throws Exception {
        Object newKeyInstance = entityMetadata.createNewKeyInstance();
        for (KeyFieldMetadata fieldMetadata : entityMetadata.getKeyFields()) {
            String propertyName = fieldMetadata.getOutputPropertyName();
            if (keyJson.has(propertyName)) {
                Object value = CommonUtil.gsonToType(keyJson.get(propertyName), fieldMetadata.getFieldType());
                fieldMetadata.setEntityFieldWithValue(newKeyInstance, value);
            } else {
                throw new BadRequestException("primary key incomplete" + keyJson);
            }
        }
        return newKeyInstance;
    }

}
