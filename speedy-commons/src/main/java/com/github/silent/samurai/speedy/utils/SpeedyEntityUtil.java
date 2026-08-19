package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.interfaces.metadata.AssociationColumn;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.util.List;
import java.util.Optional;

public class SpeedyEntityUtil {

    /// The foreign-key value an association carries in a *flat* row, derived from a **target-side**
    /// entity (a fetched target row, or the nested object a client sent).
    ///
    /// A single-column foreign key is the bare scalar it has always been; a multi-column one — an
    /// association whose target has a composite primary key — is a {@link SpeedyEntityKey} over the
    /// target's key fields. Both shapes compare by key value, so either can be used to match parent
    /// rows to fetched targets.
    ///
    /// Returns empty unless {@code target} carries a usable (present, non-null, non-empty) value for
    /// *every* column of the foreign key — a partially populated key identifies no single row.
    public static Optional<SpeedyValue> foreignKeyOf(SpeedyEntity target, FieldMetadata association) {
        List<AssociationColumn> columns = association.getAssociationColumns();
        if (columns.isEmpty()) {
            return Optional.empty();
        }
        if (columns.size() == 1) {
            FieldMetadata targetKeyField = columns.get(0).targetKeyField();
            return usableValue(target, targetKeyField);
        }
        SpeedyEntityKey key = new SpeedyEntityKey(association.getAssociationMetadata());
        for (AssociationColumn column : columns) {
            Optional<SpeedyValue> value = usableValue(target, column.targetKeyField());
            if (value.isEmpty()) {
                return Optional.empty();
            }
            key.put(column.targetKeyField(), value.get());
        }
        return Optional.of(key);
    }

    /// Expands the foreign-key value a flat row stores for {@code association} back into the target's
    /// full {@link SpeedyEntityKey} — the inverse of {@link #foreignKeyOf}. Returns empty when the
    /// foreign key is absent, null, or (for a multi-column key) missing one of its columns.
    public static Optional<SpeedyEntityKey> associationKeyOf(SpeedyValue foreignKey, FieldMetadata association) {
        List<AssociationColumn> columns = association.getAssociationColumns();
        if (columns.isEmpty() || foreignKey == null || foreignKey.isNull() || foreignKey.isEmpty()) {
            return Optional.empty();
        }
        SpeedyEntityKey key = new SpeedyEntityKey(association.getAssociationMetadata());
        if (columns.size() == 1) {
            key.put(columns.get(0).targetKeyField(), foreignKey);
            return Optional.of(key);
        }
        if (!foreignKey.isObject()) {
            return Optional.empty();
        }
        return foreignKeyOf(foreignKey.asObject(), association).map(fk -> (SpeedyEntityKey) fk.asObject());
    }

    private static Optional<SpeedyValue> usableValue(SpeedyEntity entity, FieldMetadata fieldMetadata) {
        if (!entity.has(fieldMetadata)) {
            return Optional.empty();
        }
        SpeedyValue value = entity.get(fieldMetadata);
        if (value == null || value.isNull() || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

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
