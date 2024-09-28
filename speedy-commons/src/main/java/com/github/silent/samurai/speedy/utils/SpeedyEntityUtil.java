package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

public class SpeedyEntityUtil {

    public static SpeedyEntityKey toEntityKey(final SpeedyEntity entity) {
        EntityMetadata metadata = entity.getMetadata();
        SpeedyEntityKey speedyEntityKey = new SpeedyEntityKey(metadata);
        for (KeyFieldMetadata keyField : metadata.getKeyFields()) {
            if (keyField.isAssociation()) {
                SpeedyEntity associatedEntity = entity.get(keyField).asObject();
                FieldMetadata associatedFieldMetadata = keyField.getAssociatedFieldMetadata();
                SpeedyValue innerValue = associatedEntity.get(associatedFieldMetadata);
                speedyEntityKey.put(keyField, innerValue);
            } else {
                SpeedyValue speedyValue = entity.get(keyField);
                speedyEntityKey.put(keyField, speedyValue);
            }

        }
        return speedyEntityKey;
    }
}
