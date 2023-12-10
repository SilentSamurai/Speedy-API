package com.github.silent.samurai.speedy.deserializer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

public class JsonIdentityDeserializer {

    private final EntityMetadata entityMetadata;
    private final ObjectNode keyJson;

    public JsonIdentityDeserializer(EntityMetadata entityMetadata, ObjectNode keyJson) {
        this.entityMetadata = entityMetadata;
        this.keyJson = keyJson;
    }

    public SpeedyEntityKey deserialize() throws Exception {
        return this.getCompositeKey();
    }

    private SpeedyEntityKey getCompositeKey() throws Exception {
        SpeedyEntityKey speedyEntityKey = new SpeedyEntityKey(entityMetadata);
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            String propertyName = keyFieldMetadata.getOutputPropertyName();
            if (keyJson.has(propertyName)) {
                SpeedyValue speedyValue = JsonEntityDeserializer.fromFieldMetadata(keyFieldMetadata, keyJson.get(propertyName));
                speedyEntityKey.put(keyFieldMetadata, speedyValue);
            } else {
                throw new BadRequestException("primary key incomplete" + keyJson);
            }
        }
        return speedyEntityKey;
    }

}
