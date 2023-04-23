package com.github.silent.samurai.deserializer;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.utils.CommonUtil;

import java.util.Map;
import java.util.Optional;

public class MapEntityKeyDeserializer {

    private final EntityMetadata entityMetadata;
    private final Map<String, String> keyMap;

    public MapEntityKeyDeserializer(Map<String, String> keyMap, EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
        this.keyMap = keyMap;
    }

    public Object deserialize() throws Exception {
        if (entityMetadata.hasCompositeKey()) {
            return this.getCompositeKey();
        }
        return this.getBasicKey();
    }

    private Object getBasicKey() throws BadRequestException {
        Optional<KeyFieldMetadata> primaryKeyFieldMetadata = entityMetadata.getKeyFields().stream().findAny();
        if (primaryKeyFieldMetadata.isPresent()) {
            KeyFieldMetadata keyFieldMetadata = primaryKeyFieldMetadata.get();
            String propertyName = keyFieldMetadata.getOutputPropertyName();
            if (keyMap.containsKey(propertyName)) {
                return CommonUtil.stringToType(keyMap.get(propertyName), keyFieldMetadata.getFieldType());
            }
        }
        throw new BadRequestException("primary key field not found" + keyMap);
    }

    private Object getCompositeKey() throws Exception {
        Object newKeyInstance = entityMetadata.createNewKeyInstance();
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            String propertyName = keyFieldMetadata.getOutputPropertyName();
            if (keyMap.containsKey(propertyName)) {
                Object value = CommonUtil.stringToType(keyMap.get(propertyName), keyFieldMetadata.getFieldType());
                keyFieldMetadata.setEntityFieldWithValue(newKeyInstance, value);
            } else {
                throw new BadRequestException("primary key incomplete" + keyMap);
            }
        }
        return newKeyInstance;
    }

}
