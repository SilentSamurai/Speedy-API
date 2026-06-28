package com.github.silent.samurai.speedy.walker;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.RowReader;
import com.github.silent.samurai.speedy.models.ExpansionPathTracker;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.Speedy;

import java.util.Optional;
import java.util.Set;

/// Format-agnostic builder: *enriches* a backend's *flat* row {@link SpeedyEntity} (scalar fields
/// plus each association's foreign-key value) into the resolved entity, replacing each association's
/// FK scalar with either a nested entity ({@code $expand}, depth/cycle controlled by
/// {@link ExpansionPathTracker}) or a keys-only reference. The read-side mirror of
/// {@link SpeedyToRecord} and the persistence analogue of
/// {@code com.github.silent.samurai.speedy.serialization.StructureToSpeedy}.
///
/// The flat row is freshly built by the backend per record and used nowhere else, so enrichment is
/// done **in place** and the same instance is returned — no parallel tree is allocated. Purely
/// structural: it reads already-decoded {@link SpeedyValue}s and navigates foreign keys via
/// {@link RowReader#selectByFk}; value decoding lives in the backend port, so this walker holds no
/// {@code TypeConverter}. Extracted from the former jOOQ {@code JooqSqlToSpeedy} with no behavioural change.
public class RecordToSpeedy {

    private final RowReader rowReader;

    public RecordToSpeedy(RowReader rowReader) {
        this.rowReader = rowReader;
    }

    public SpeedyEntity fromRow(SpeedyEntity row, EntityMetadata from, Set<String> expand) throws SpeedyHttpException {
        ExpansionPathTracker pathTracker = new ExpansionPathTracker(expand);
        return enrich(row, from, pathTracker);
    }

    /// Enriches the flat {@code row} in place — resolving associations and back-filling any absent
    /// scalar field with null — and returns the same instance.
    private SpeedyEntity enrich(SpeedyEntity row, EntityMetadata entityMetadata, ExpansionPathTracker pathTracker) throws SpeedyHttpException {
        // Push the current entity onto the path tracker
        pathTracker.pushEntity(entityMetadata);

        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (fieldMetadata.isAssociation()) {
                // Check if this specific path should be expanded using dot notation
                if (pathTracker.shouldExpand(fieldMetadata.getAssociationMetadata())) {
                    // read the FK off the row, fetch the related row, then replace the FK with it
                    Optional<SpeedyEntity> associatedRow = rowReader.selectByFk(fieldMetadata, row);
                    // if fk is null
                    if (associatedRow.isEmpty()) {
                        row.put(fieldMetadata, Speedy.fromNull());
                        continue;
                    }
                    if (fieldMetadata.isCollection()) {
                        throw new BadRequestException("operation not supported");
                    } else {
                        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                        SpeedyEntity associatedEntity = enrich(
                                associatedRow.get(),
                                associationMetadata,
                                pathTracker
                        );
                        row.put(fieldMetadata, associatedEntity);
                    }
                } else {
                    if (fieldMetadata.isCollection()) {
                        throw new BadRequestException("operation not supported");
                    } else {
                        Optional<SpeedyEntityKey> associatedEntityKey = createSpeedyKeyFromFK(row, fieldMetadata);
                        if (associatedEntityKey.isEmpty() || associatedEntityKey.get().isEmpty()) {
                            row.put(fieldMetadata, Speedy.fromNull());
                            continue;
                        }
                        row.put(fieldMetadata, associatedEntityKey.get());
                    }
                }
            } else if (!row.has(fieldMetadata)) {
                // scalar already present in the flat row; only back-fill the missing ones
                row.put(fieldMetadata, Speedy.fromNull());
            }
        }

        // Pop the current entity from the path tracker when done processing
        pathTracker.popEntity();
        return row;
    }

    public Optional<SpeedyEntityKey> createSpeedyKeyFromFK(SpeedyEntity row, FieldMetadata fieldMetadata) throws SpeedyHttpException {
        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        KeyFieldMetadata keyFieldMetadata = associationMetadata.getKeyFields().stream().findAny().orElseThrow();
        // foreign key column, decoded with the associated field's type by the backend
        if (!row.has(fieldMetadata)) {
            return Optional.empty();
        }
        SpeedyValue fk = row.get(fieldMetadata);
        SpeedyEntityKey speedyEntityKey = new SpeedyEntityKey(associationMetadata);
        speedyEntityKey.put(keyFieldMetadata, fk);

        return Optional.of(speedyEntityKey);
    }
}
