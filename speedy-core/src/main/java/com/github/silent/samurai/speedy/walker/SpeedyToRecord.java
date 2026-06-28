package com.github.silent.samurai.speedy.walker;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.RowWriter;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyText;

import java.util.UUID;

/// Format-agnostic walker that flattens a source {@link SpeedyEntity} into a *column*
/// set of fields — the fields whose backing columns a
/// {@link RowWriter} should write.
/// Mutates the entity **in place**: removes key fields (for updates), flattens associations
/// to FK scalars, strips null/empty values, and injects generated UUIDs.
/// The write-side mirror of {@link RecordToSpeedy} and the persistence analogue of
/// {@code com.github.silent.samurai.speedy.serialization.ResponseWalker}.
///
/// Owns only the genuinely reusable, backend-neutral logic: application-side key generation,
/// skipping empty/null values, and flattening an association to the {@link SpeedyValue} of its
/// foreign-key target. It deliberately does **not** convert values to column types — that leaf
/// codec lives in the backend port (the {@code RowWriter}), so the entity stays in
/// {@link SpeedyValue} currency.
public class SpeedyToRecord {

    /// Flattens the entity for INSERT in place. Generated keys (UUID) are produced here and written
    /// directly onto {@code entity} so the caller can refetch by primary key afterwards.
    /// Returns the same instance.
    public SpeedyEntity toInsertColumns(SpeedyEntity entity) throws SpeedyHttpException {
        EntityMetadata entityMetadata = entity.getMetadata();

        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (fieldMetadata instanceof KeyFieldMetadata && ((KeyFieldMetadata) fieldMetadata).shouldGenerateKey()) {
                entity.put(fieldMetadata, new SpeedyText(UUID.randomUUID().toString()));
            } else {
                if (!entity.has(fieldMetadata) || entity.get(fieldMetadata).isEmpty() || entity.get(fieldMetadata).isNull()) {
                    if (entity.has(fieldMetadata)) {
                        entity.remove(fieldMetadata);
                    }
                    continue;
                }
                flattenAssociation(entity, fieldMetadata);
            }
        }
        return entity;
    }

    /// Flattens the entity for UPDATE in place over all non-key fields. Removes key fields from
    /// the entity and flattens association non-key fields to FK scalars. Returns the same instance.
    public SpeedyEntity toUpdateColumns(SpeedyEntity entity) throws SpeedyHttpException {
        EntityMetadata entityMetadata = entity.getMetadata();

        for (KeyFieldMetadata keyField : entityMetadata.getKeyFields()) {
            if (entity.has(keyField)) {
                entity.remove(keyField);
            }
        }

        for (FieldMetadata fieldMetadata : entityMetadata.getAllNonKeyFields()) {
            if (entity.has(fieldMetadata)
                    && !entity.get(fieldMetadata).isEmpty()
                    && !entity.get(fieldMetadata).isNull()) {
                flattenAssociation(entity, fieldMetadata);
            } else if (entity.has(fieldMetadata)) {
                entity.remove(fieldMetadata);
            }
        }
        return entity;
    }

    private void flattenAssociation(SpeedyEntity entity, FieldMetadata fieldMetadata) throws SpeedyHttpException {
        if (!fieldMetadata.isAssociation()) {
            return;
        }
        SpeedyEntity associatedEntity = entity.get(fieldMetadata).asObject();
        FieldMetadata associatedFieldMetadata = fieldMetadata.getAssociatedFieldMetadata();
        if (!associatedEntity.has(associatedFieldMetadata)
                || associatedEntity.get(associatedFieldMetadata).isEmpty()
                || associatedEntity.get(associatedFieldMetadata).isNull()) {
            entity.remove(fieldMetadata);
            return;
        }
        entity.put(fieldMetadata, associatedEntity.get(associatedFieldMetadata));
    }
}
