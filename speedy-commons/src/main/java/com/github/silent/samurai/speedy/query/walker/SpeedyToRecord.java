package com.github.silent.samurai.speedy.query.walker;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyText;

import java.util.UUID;

/// Format-agnostic walker that flattens a source {@link SpeedyEntity} into a *column*
/// {@link SpeedyEntity} — the set of fields whose backing columns a
/// {@link com.github.silent.samurai.speedy.interfaces.query.backend.RowWriter} should write. The
/// write-side mirror of {@link RecordToSpeedy} and the persistence analogue of
/// {@code com.github.silent.samurai.speedy.serialization.ResponseWalker}.
///
/// Owns only the genuinely reusable, backend-neutral logic: application-side key generation,
/// skipping empty/null values, and flattening an association to the {@link SpeedyValue} of its
/// foreign-key target. It deliberately does **not** convert values to column types — that leaf
/// codec lives in the backend port (the {@code RowWriter}), so the column entity stays in
/// {@link SpeedyValue} currency.
public class SpeedyToRecord {

    /// Builds the column entity for an INSERT. Generated keys (UUID) are produced here and written
    /// back onto {@code entity} so the caller can refetch by primary key afterwards.
    public SpeedyEntity toInsertColumns(SpeedyEntity entity) throws SpeedyHttpException {
        EntityMetadata entityMetadata = entity.getMetadata();
        SpeedyEntity columns = new SpeedyEntity(entityMetadata);

        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (fieldMetadata instanceof KeyFieldMetadata && ((KeyFieldMetadata) fieldMetadata).shouldGenerateKey()) {
                SpeedyText value = new SpeedyText(UUID.randomUUID().toString());
                columns.put(fieldMetadata, value);
                entity.put(fieldMetadata, value);
            } else {
                if (!entity.has(fieldMetadata) || entity.get(fieldMetadata).isEmpty() || entity.get(fieldMetadata).isNull()) {
                    // you can throw by checking nullable or db can throw
                    continue;
                }
                putColumn(columns, fieldMetadata, entity.get(fieldMetadata));
            }
        }
        return columns;
    }

    /// Builds the column entity for an UPDATE over all non-key fields.
    public SpeedyEntity toUpdateColumns(SpeedyEntity entity) throws SpeedyHttpException {
        EntityMetadata entityMetadata = entity.getMetadata();
        SpeedyEntity columns = new SpeedyEntity(entityMetadata);

        for (FieldMetadata fieldMetadata : entityMetadata.getAllNonKeyFields()) {
            if (entity.has(fieldMetadata)
                    && !entity.get(fieldMetadata).isEmpty()
                    && !entity.get(fieldMetadata).isNull()) {
                putColumn(columns, fieldMetadata, entity.get(fieldMetadata));
            }
        }
        return columns;
    }

    private void putColumn(SpeedyEntity columns, FieldMetadata fieldMetadata, SpeedyValue speedyValue) throws SpeedyHttpException {
        if (fieldMetadata.isAssociation()) {
            SpeedyEntity associatedEntity = speedyValue.asObject();
            FieldMetadata associatedFieldMetadata = fieldMetadata.getAssociatedFieldMetadata();
            if (!associatedEntity.has(associatedFieldMetadata)
                    || associatedEntity.get(associatedFieldMetadata).isEmpty()
                    || associatedEntity.get(associatedFieldMetadata).isNull()) {
                // missing FK target — leave the column unset (DB/nullable check decides)
                return;
            }
            // Store the foreign key's SpeedyValue keyed by the association field; the backend
            // resolves it to the FK column and converts it (using the associated field's type).
            columns.put(fieldMetadata, associatedEntity.get(associatedFieldMetadata));
        } else {
            columns.put(fieldMetadata, speedyValue);
        }
    }
}
