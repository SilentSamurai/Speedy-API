package com.github.silent.samurai.speedy.backend;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.NotImplementedException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.backend.RowReader;
import com.github.silent.samurai.speedy.models.ExpansionPathTracker;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.Speedy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
/// done **in place**, and the same instance is returned — no parallel tree is allocated. Purely
/// structural: it reads already-decoded {@link SpeedyValue}s and navigates foreign keys via
/// {@link RowReader#selectByFks}; value decoding lives in the backend port, so this walker holds no
/// {@code TypeConverter}. Extracted from the former jOOQ {@code JooqSqlToSpeedy} with no behavioural change.
public class RecordToSpeedy {

    /// Upper bound on how many foreign keys go into a single fetch. Oracle rejects an IN list longer
    /// than 1000 elements, and other backends have their own bind-parameter ceilings.
    private static final int FK_BATCH_SIZE = 500;

    private final RowReader rowReader;

    public RecordToSpeedy(RowReader rowReader) {
        this.rowReader = rowReader;
    }

    public SpeedyEntity fromRow(SpeedyEntity row, EntityMetadata from, Set<String> expand) throws SpeedyHttpException {
        return fromRows(List.of(row), from, expand).get(0);
    }

    /// Enriches every row of one result set together, so each `$expand` level costs one fetch for the
    /// whole set rather than one per row. Rows are enriched in place and returned as the same list.
    public List<SpeedyEntity> fromRows(List<SpeedyEntity> rows, EntityMetadata from, Set<String> expand)
            throws SpeedyHttpException {
        ExpansionPathTracker pathTracker = new ExpansionPathTracker(expand);
        enrich(rows, from, pathTracker);
        return rows;
    }

    /// Enriches the flat {@code rows} in place — resolving associations and back-filling any absent
    /// scalar field with null.
    ///
    /// Walks breadth-first: every row at one position in the entity tree is processed together, so the
    /// complete set of foreign keys for the next level is known before that level is fetched. A
    /// depth-first walk cannot batch, because row 2's foreign key is not read until row 1 has already
    /// been resolved to full depth.
    private void enrich(List<SpeedyEntity> rows, EntityMetadata entityMetadata, ExpansionPathTracker pathTracker) throws SpeedyHttpException {
        if (rows.isEmpty()) {
            return;
        }
        // Push the current entity onto the path tracker
        pathTracker.pushEntity(entityMetadata);

        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (fieldMetadata.isAssociation()) {
                // Check if this specific path should be expanded using dot notation
                if (pathTracker.shouldExpand(fieldMetadata.getAssociationMetadata())) {
                    expandAssociation(rows, fieldMetadata, entityMetadata, pathTracker);
                } else {
                    if (fieldMetadata.isCollection()) {
                        throw new NotImplementedException("Collection association key reference is not supported for field '"
                                + fieldMetadata.getOutputPropertyName() + "' on entity '" + entityMetadata.getName() + "'");
                    }
                    for (SpeedyEntity row : rows) {
                        Optional<SpeedyEntityKey> associatedEntityKey = createSpeedyKeyFromFK(row, fieldMetadata);
                        if (associatedEntityKey.isEmpty() || associatedEntityKey.get().isEmpty()) {
                            row.put(fieldMetadata, Speedy.fromNull());
                            continue;
                        }
                        row.put(fieldMetadata, associatedEntityKey.get());
                    }
                }
            } else {
                for (SpeedyEntity row : rows) {
                    // scalar already present in the flat row; only back-fill the missing ones
                    if (!row.has(fieldMetadata)) {
                        row.put(fieldMetadata, Speedy.fromNull());
                    }
                }
            }
        }

        // Pop the current entity from the path tracker when done processing
        pathTracker.popEntity();
    }

    /// Resolves one expanded association for the whole row set: collect the distinct foreign keys,
    /// fetch their targets in batches, enrich those targets (recursing a whole level at a time), then
    /// replace each row's foreign key with the target it resolved to.
    private void expandAssociation(List<SpeedyEntity> rows,
                                   FieldMetadata association,
                                   EntityMetadata entityMetadata,
                                   ExpansionPathTracker pathTracker) throws SpeedyHttpException {
        // LinkedHashSet: distinct keys, but a stable batch order so the emitted query is reproducible.
        Set<SpeedyValue> distinctFks = new LinkedHashSet<>();
        for (SpeedyEntity row : rows) {
            SpeedyValue fk = foreignKeyOf(row, association);
            if (fk != null) {
                distinctFks.add(fk);
            }
        }

        if (distinctFks.isEmpty()) {
            // No row carries a foreign key — nothing to resolve, and nothing to fetch.
            for (SpeedyEntity row : rows) {
                row.put(association, Speedy.fromNull());
            }
            return;
        }

        if (association.isCollection()) {
            throw new NotImplementedException("Collection association expansion is not supported for field '"
                    + association.getOutputPropertyName() + "' on entity '" + entityMetadata.getName() + "'");
        }

        FieldMetadata associatedField = association.getAssociatedFieldMetadata();
        Map<SpeedyValue, SpeedyEntity> targetsByFk = new HashMap<>();
        List<SpeedyEntity> targets = new ArrayList<>();

        // Chunked so the emitted IN list stays within backend limits (Oracle caps it at 1000 elements,
        // and a page can carry more keys than that).
        List<SpeedyValue> fkBatch = new ArrayList<>(distinctFks);
        for (int start = 0; start < fkBatch.size(); start += FK_BATCH_SIZE) {
            List<SpeedyValue> chunk = fkBatch.subList(start, Math.min(start + FK_BATCH_SIZE, fkBatch.size()));
            for (SpeedyEntity target : rowReader.selectByFks(association, chunk)) {
                if (!target.has(associatedField)) {
                    continue;
                }
                // First row wins per foreign key, matching the single-row fetch this replaces.
                if (targetsByFk.putIfAbsent(target.get(associatedField), target) == null) {
                    targets.add(target);
                }
            }
        }

        // Recurse on the deduplicated targets, so the next level is also one fetch for the whole set.
        enrich(targets, association.getAssociationMetadata(), pathTracker);

        for (SpeedyEntity row : rows) {
            SpeedyValue fk = foreignKeyOf(row, association);
            SpeedyEntity target = fk == null ? null : targetsByFk.get(fk);
            // An unresolved foreign key (absent column, SQL NULL, or no matching target row) reads as
            // null, exactly as the per-row fetch did.
            row.put(association, target == null ? Speedy.fromNull() : target);
        }
    }

    /// The usable foreign-key value {@code association} carries on {@code row}, or null when the row
    /// has no value to follow — the column was not selected, or it is SQL NULL.
    private SpeedyValue foreignKeyOf(SpeedyEntity row, FieldMetadata association) {
        if (!row.has(association)) {
            return null;
        }
        SpeedyValue fk = row.get(association);
        if (fk == null || fk.isNull() || fk.isEmpty()) {
            return null;
        }
        return fk;
    }

    public Optional<SpeedyEntityKey> createSpeedyKeyFromFK(SpeedyEntity row, FieldMetadata fieldMetadata) throws SpeedyHttpException {
        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        KeyFieldMetadata keyFieldMetadata = associationMetadata.getKeyFields().stream().findAny()
                .orElseThrow(() -> new InternalServerError(
                        "Associated entity '" + associationMetadata.getName() + "' has no key fields"));
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
