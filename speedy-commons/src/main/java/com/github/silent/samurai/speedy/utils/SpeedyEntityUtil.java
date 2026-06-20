package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

public class SpeedyEntityUtil {

    /// Extracts the primary-key values from a {@link SpeedyEntity} into a {@link SpeedyEntityKey}.
    /// For each declared key field in the entity's metadata:
    ///   - Association key fields: if the value is a nested object, the actual FK value is resolved
    ///     from the associated entity's referenced field; otherwise the scalar value is used directly.
    ///   - Regular key fields: the value is copied as-is.
    public static SpeedyEntityKey toEntityKey(final SpeedyEntity entity) {
        EntityMetadata metadata = entity.getMetadata();
        SpeedyEntityKey speedyEntityKey = new SpeedyEntityKey(metadata);
        for (KeyFieldMetadata keyField : metadata.getKeyFields()) {
            if (keyField.isAssociation()) {
                SpeedyValue val = entity.get(keyField);
                if (val.isObject()) {
                    SpeedyEntity associatedEntity = val.asObject();
                    FieldMetadata associatedFieldMetadata = keyField.getAssociatedFieldMetadata();
                    speedyEntityKey.put(keyField, associatedEntity.get(associatedFieldMetadata));
                } else {
                    speedyEntityKey.put(keyField, val);
                }
            } else {
                SpeedyValue speedyValue = entity.get(keyField);
                speedyEntityKey.put(keyField, speedyValue);
            }
        }
        return speedyEntityKey;
    }
}
