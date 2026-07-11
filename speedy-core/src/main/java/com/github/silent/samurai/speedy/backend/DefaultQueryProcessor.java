package com.github.silent.samurai.speedy.backend;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.conditions.BinaryConditionImpl;
import com.github.silent.samurai.speedy.models.conditions.NormalField;
import com.github.silent.samurai.speedy.utils.Speedy;
import com.github.silent.samurai.speedy.utils.SpeedyEntityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/// Format-agnostic {@link QueryProcessor}: owns CRUD orchestration (query execution, the
/// creation/delete refetch-by-primary-key loops, update, exists, transaction handling, and native
/// exception mapping) while delegating every backend-specific operation to a {@link SpeedyBackend}
/// port. The persistence-side mirror of
/// {@code com.github.silent.samurai.speedy.serialization.DefaultResponseSerializer}: the only
/// backend-specific piece is the {@link SpeedyBackend} port (which owns its own value conversion), so
/// a new backend reuses all the logic here.
public class DefaultQueryProcessor implements QueryProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryProcessor.class);

    private final SpeedyBackend backend;
    private final RecordToSpeedy recordToSpeedy;
    private final SpeedyToRecord speedyToRecord;

    public DefaultQueryProcessor(SpeedyBackend backend) {
        this.backend = backend;
        this.recordToSpeedy = new RecordToSpeedy(backend);
        this.speedyToRecord = new SpeedyToRecord();
    }

    @Override
    public BigInteger executeCount(SpeedyQuery query) throws SpeedyHttpException {
        try {
            applyDeletedFilter(query);
            return backend.count(query);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> executeMany(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            applyDeletedFilter(speedyQuery);
            return mapRows(backend.select(speedyQuery), speedyQuery);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public QueryResult executeManyWithCount(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            applyDeletedFilter(speedyQuery);
            BigInteger totalCount = backend.count(speedyQuery);
            List<SpeedyEntity> list = mapRows(backend.select(speedyQuery), speedyQuery);
            return new QueryResult(list, totalCount);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public boolean exists(SpeedyEntityKey entityKey) throws SpeedyHttpException {
        try {
            // For a soft-delete entity a soft-deleted row is treated as absent by normal CRUD
            // (a second $delete, or an $update/$replace, on a deleted row → 404). Restore/purge
            // deliberately bypass this by fetching the raw row themselves.
            if (entityKey.getMetadata().isSoftDeleteEnabled()) {
                return !findExistingKeys(List.of(entityKey)).isEmpty();
            }
            return backend.existsByKey(entityKey);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public Set<SpeedyEntityKey> findExistingKeys(List<SpeedyEntityKey> keys) throws SpeedyHttpException {
        if (keys.isEmpty()) {
            return Set.of();
        }
        try {
            EntityMetadata entityMetadata = keys.get(0).getMetadata();
            FieldMetadata softDeleteField = entityMetadata.getSoftDeleteField();
            List<SpeedyEntity> rows = backend.selectByKeys(keys);
            Set<SpeedyEntityKey> existing = new HashSet<>(rows.size());
            for (SpeedyEntity row : rows) {
                // A soft-deleted row is "not found" for normal CRUD existence checks.
                if (softDeleteField != null && isSoftDeleted(row, softDeleteField)) {
                    continue;
                }
                existing.add(SpeedyEntityUtil.toEntityKey(
                        recordToSpeedy.fromRow(row, entityMetadata, Set.of())));
            }
            return existing;
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> create(List<SpeedyEntity> entities) throws SpeedyHttpException {
        try {
            if (entities.isEmpty()) {
                return new ArrayList<>();
            }

            for (SpeedyEntity entity : entities) {
                speedyToRecord.toInsertColumns(entity);
            }
            backend.insert(entities);

            // Which keys the database assigns is backend-neutral; the backend only owns the read-back
            // mechanism. Hold every backend to the contract here so a missed read-back fails loudly and
            // precisely instead of silently yielding an incomplete key (and a 200 with empty payload).
            for (SpeedyEntity entity : entities) {
                EntityMetadata entityMetadata = entity.getMetadata();
                // The backend skips an entity with no columns to insert (buildInsertStep == null); such an
                // entity has no row and no DB-generated key to read back, so don't hold it to the contract.
                boolean hasColumnToInsert = entityMetadata.getAllFields().stream().anyMatch(entity::has);
                if (!hasColumnToInsert) {
                    continue;
                }
                Optional<KeyFieldMetadata> missingKey =
                        MetadataUtil.findUnpopulatedDatabaseGeneratedKey(entityMetadata, entity);
                if (missingKey.isPresent()) {
                    throw new InternalServerError("Persistence backend did not return database-generated key '"
                            + missingKey.get().getOutputPropertyName() + "' after insert");
                }
            }

            List<SpeedyEntityKey> keys = new ArrayList<>(entities.size());
            for (SpeedyEntity entity : entities) {
                keys.add(SpeedyEntityUtil.toEntityKey(entity));
            }
            return refetchInKeyOrder(keys, entities.get(0).getMetadata());
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        try {
            // PATCH: only the supplied non-key fields are written; omitted fields are stripped.
            speedyToRecord.toUpdateColumns(entity);
            return writeAndRefetch(pk, entity);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public SpeedyEntity replace(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        try {
            // PUT: full replace — omitted nullable non-key columns are reset to null.
            speedyToRecord.toReplaceColumns(entity);
            return writeAndRefetch(pk, entity);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    /// Writes the already-flattened entity by primary key and refetches the persisted row.
    /// Shared by {@link #update} (PATCH) and {@link #replace} (PUT); the only difference between
    /// them is which flatten (partial vs full-replace) runs before this.
    private SpeedyEntity writeAndRefetch(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        backend.update(pk, entity);

        List<SpeedyEntity> rows = backend.selectByKeys(List.of(pk));
        if (rows.isEmpty()) {
            throw new NotFoundException("Entity not found for PK: " + pk);
        }
        return recordToSpeedy.fromRow(rows.get(0), entity.getMetadata(), Set.of());
    }

    @Override
    public List<SpeedyEntity> delete(List<SpeedyEntityKey> pks) throws SpeedyHttpException {
        try {
            // Nothing to delete: never hand the backend an empty key set (no WHERE -> table-wipe risk).
            if (pks.isEmpty()) {
                return new ArrayList<>();
            }

            EntityMetadata entityMetadata = pks.get(0).getMetadata();
            // Deleting "by keys" with no key fields would produce no WHERE clause and wipe the table.
            // This safety invariant must hold for every backend, so enforce it here rather than relying
            // on each impl to re-implement the guard.
            if (entityMetadata.getKeyFields().isEmpty()) {
                LOGGER.warn("Refusing key-less delete for entity '{}' (no key fields)", entityMetadata.getName());
                return new ArrayList<>();
            }

            if (entityMetadata.isSoftDeleteEnabled()) {
                return softDelete(pks, entityMetadata);
            }
            return hardDelete(pks, entityMetadata);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> purge(List<SpeedyEntityKey> pks) throws SpeedyHttpException {
        try {
            if (pks.isEmpty()) {
                return new ArrayList<>();
            }
            EntityMetadata entityMetadata = pks.get(0).getMetadata();
            if (entityMetadata.getKeyFields().isEmpty()) {
                LOGGER.warn("Refusing key-less purge for entity '{}' (no key fields)", entityMetadata.getName());
                return new ArrayList<>();
            }
            // Purge targets any existing row (live or soft-deleted), so it uses a raw refetch and
            // requires every key to exist — 404 on a missing key, mirroring $delete.
            List<SpeedyEntity> entities = refetchInKeyOrder(pks, entityMetadata);
            if (entities.size() != pks.size()) {
                Set<SpeedyEntityKey> found = new HashSet<>(entities.size());
                for (SpeedyEntity entity : entities) {
                    found.add(SpeedyEntityUtil.toEntityKey(entity));
                }
                for (SpeedyEntityKey pk : pks) {
                    if (!found.contains(pk)) {
                        throw new NotFoundException("entity not found: " + pk);
                    }
                }
            }
            // Unconditional hard delete, regardless of soft-delete configuration.
            backend.deleteByKeys(pks);
            return entities;
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> restore(List<SpeedyEntityKey> pks) throws SpeedyHttpException {
        try {
            if (pks.isEmpty()) {
                return new ArrayList<>();
            }
            EntityMetadata entityMetadata = pks.get(0).getMetadata();
            FieldMetadata softDeleteField = entityMetadata.getSoftDeleteField();
            if (softDeleteField == null) {
                throw new InternalServerError("restore called on non-soft-delete entity '"
                        + entityMetadata.getName() + "'");
            }
            // Restore targets rows in the deleted set, so it uses the raw (unfiltered) row and
            // requires each key to identify a currently soft-deleted row.
            SpeedyValue cleared = notDeletedMarker(softDeleteField);
            for (SpeedyEntityKey pk : pks) {
                List<SpeedyEntity> raw = backend.selectByKeys(List.of(pk));
                if (raw.isEmpty() || !isSoftDeleted(raw.get(0), softDeleteField)) {
                    throw new NotFoundException("soft-deleted entity not found: " + pk);
                }
                SpeedyEntity update = new SpeedyEntity(entityMetadata);
                update.put(softDeleteField, cleared);
                // Write the cleared marker directly: toUpdateColumns() would strip a null value,
                // which is exactly the value that clears a temporal marker.
                backend.update(pk, update);
            }
            return refetchInKeyOrder(pks, entityMetadata);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    /// Sets the soft-delete marker on each row (reuses the backend's update mechanism), then
    /// refetches so the response and POST_DELETE payload carry the post-delete state.
    private List<SpeedyEntity> softDelete(List<SpeedyEntityKey> pks, EntityMetadata entityMetadata)
            throws SpeedyHttpException {
        FieldMetadata softDeleteField = entityMetadata.getSoftDeleteField();
        SpeedyValue marker = deletedMarker(softDeleteField);
        for (SpeedyEntityKey pk : pks) {
            SpeedyEntity update = new SpeedyEntity(entityMetadata);
            update.put(softDeleteField, marker);
            // Write the marker directly (no toUpdateColumns): the entity carries only the marker
            // scalar, so there is nothing to flatten and a null marker must not be stripped.
            backend.update(pk, update);
        }
        return refetchInKeyOrder(pks, entityMetadata);
    }

    /// Unconditional hard delete: refetch (so the response/POST_DELETE payload has the full row as
    /// it existed) then remove.
    private List<SpeedyEntity> hardDelete(List<SpeedyEntityKey> pks, EntityMetadata entityMetadata)
            throws SpeedyHttpException {
        List<SpeedyEntity> entities = refetchInKeyOrder(pks, entityMetadata);
        backend.deleteByKeys(pks);
        return entities;
    }

    @Override
    public void runInTransaction(Runnable block) {
        backend.runInTransaction(block);
    }

    /// Rebuilds each backend row into a {@link SpeedyEntity}, preserving the backend's row order.
    private List<SpeedyEntity> mapRows(List<SpeedyEntity> rows, SpeedyQuery query) throws SpeedyHttpException {
        List<SpeedyEntity> list = new ArrayList<>(rows.size());
        for (SpeedyEntity row : rows) {
            list.add(recordToSpeedy.fromRow(row, query.getFrom(), query.getExpand()));
        }
        return list;
    }

    /// Refetches rows for the given keys and returns the rebuilt entities in the same order as
    /// {@code keys}, dropping (and warning about) any key the backend did not return.
    private List<SpeedyEntity> refetchInKeyOrder(List<SpeedyEntityKey> keys, EntityMetadata entityMetadata)
            throws SpeedyHttpException {
        List<SpeedyEntity> rows = backend.selectByKeys(keys);
        Map<SpeedyEntityKey, SpeedyEntity> entityMap = new HashMap<>(rows.size());
        for (SpeedyEntity row : rows) {
            SpeedyEntity entity = recordToSpeedy.fromRow(row, entityMetadata, Set.of());
            entityMap.put(SpeedyEntityUtil.toEntityKey(entity), entity);
        }

        List<SpeedyEntity> ordered = new ArrayList<>(keys.size());
        for (SpeedyEntityKey key : keys) {
            SpeedyEntity entity = entityMap.get(key);
            if (entity != null) {
                ordered.add(entity);
            }
        }

        if (ordered.size() != keys.size()) {
            LOGGER.warn("selectByKeys returned {} results for {} keys for entity '{}'",
                    ordered.size(), keys.size(), entityMetadata.getName());
        }
        return ordered;
    }

    /// Restricts a read query to the soft-delete visibility the request asked for. No-op for
    /// hard-delete entities and for {@code $deleted=include}. The condition is ANDed into the
    /// query's WHERE (via {@link SpeedyQuery#restrictWith}) so every backend pushes it into SQL.
    private void applyDeletedFilter(SpeedyQuery query) throws SpeedyHttpException {
        EntityMetadata entityMetadata = query.getFrom();
        if (!entityMetadata.isSoftDeleteEnabled()) {
            return;
        }
        FieldMetadata softDeleteField = entityMetadata.getSoftDeleteField();
        Condition condition = switch (query.getDeleted()) {
            case EXCLUDE -> notDeletedCondition(softDeleteField);
            case ONLY -> deletedCondition(softDeleteField);
            case INCLUDE -> null;
        };
        if (condition != null) {
            query.restrictWith(condition);
        }
    }

    /// Condition matching rows that are NOT soft-deleted (marker IS NULL / = false).
    private Condition notDeletedCondition(FieldMetadata softDeleteField) throws SpeedyHttpException {
        QueryField field = new NormalField(softDeleteField);
        if (softDeleteField.getValueType() == ValueType.BOOL) {
            return new BinaryConditionImpl(field, ConditionOperator.EQ, new Literal(Speedy.from(Boolean.FALSE)));
        }
        return new BinaryConditionImpl(field, ConditionOperator.ISNULL, new Literal(Speedy.from(Boolean.TRUE)));
    }

    /// Condition matching rows that ARE soft-deleted (marker IS NOT NULL / = true).
    private Condition deletedCondition(FieldMetadata softDeleteField) throws SpeedyHttpException {
        QueryField field = new NormalField(softDeleteField);
        if (softDeleteField.getValueType() == ValueType.BOOL) {
            return new BinaryConditionImpl(field, ConditionOperator.EQ, new Literal(Speedy.from(Boolean.TRUE)));
        }
        return new BinaryConditionImpl(field, ConditionOperator.ISNOTNULL, new Literal(Speedy.from(Boolean.TRUE)));
    }

    /// The value written to the marker to mark a row deleted: {@code true} for a boolean marker,
    /// the current instant for a temporal marker (matched to the field's value type).
    private SpeedyValue deletedMarker(FieldMetadata softDeleteField) {
        return switch (softDeleteField.getValueType()) {
            case BOOL -> Speedy.from(Boolean.TRUE);
            case DATE -> Speedy.from(LocalDate.now());
            case DATE_TIME -> Speedy.from(LocalDateTime.now());
            case ZONED_DATE_TIME -> Speedy.from(ZonedDateTime.now());
            default -> throw new IllegalStateException(
                    "Unsupported soft-delete field type: " + softDeleteField.getValueType());
        };
    }

    /// The value written to the marker to mark a row live again: {@code false} for a boolean
    /// marker, SQL NULL for a temporal marker.
    private SpeedyValue notDeletedMarker(FieldMetadata softDeleteField) {
        if (softDeleteField.getValueType() == ValueType.BOOL) {
            return Speedy.from(Boolean.FALSE);
        }
        return Speedy.fromNull();
    }

    /// Whether a raw (flat) row is currently soft-deleted: a boolean marker set to true, or a
    /// temporal marker present and non-null.
    private boolean isSoftDeleted(SpeedyEntity row, FieldMetadata softDeleteField) {
        if (!row.has(softDeleteField)) {
            return false;
        }
        SpeedyValue value = row.get(softDeleteField);
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return Boolean.TRUE.equals(value.asBoolean());
        }
        return true;
    }

    private SpeedyHttpException wrap(String message, Exception cause) {
        if (cause instanceof SpeedyHttpException she) {
            return she;
        }
        if (cause instanceof SpeedyHttpRuntimeException re) {
            return new SpeedyHttpException(re.getStatus(), re.getMessage(), re);
        }
        return backend.classify(cause).orElseGet(() -> new InternalServerError(message, cause));
    }
}
