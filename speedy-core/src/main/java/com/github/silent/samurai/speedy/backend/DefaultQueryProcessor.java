package com.github.silent.samurai.speedy.backend;

import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.SpeedyEntityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/// Format-agnostic {@link QueryProcessor}: owns CRUD orchestration (query execution, the
/// create/delete refetch-by-primary-key loops, update, exists, transaction handling, and native
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
            return backend.count(query);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> executeMany(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            return mapRows(backend.select(speedyQuery), speedyQuery);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public QueryResult executeManyWithCount(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
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
            return backend.existsByKey(entityKey);
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> create(List<SpeedyEntity> entities) throws SpeedyHttpException {
        try {
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

            if (entities.isEmpty()) {
                return new ArrayList<>();
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
            speedyToRecord.toUpdateColumns(entity);
            backend.update(pk, entity);

            List<SpeedyEntity> rows = backend.selectByKeys(List.of(pk));
            if (rows.isEmpty()) {
                throw new NotFoundException("Entity not found for PK: " + pk);
            }
            return recordToSpeedy.fromRow(rows.get(0), entity.getMetadata(), Set.of());
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
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

            List<SpeedyEntity> entities = refetchInKeyOrder(pks, entityMetadata);
            backend.deleteByKeys(pks);
            return entities;
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
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

    private SpeedyHttpException wrap(String message, Exception cause) {
        if (cause instanceof SpeedyHttpException she) {
            return she;
        }
        if (cause instanceof SpeedyHttpRuntimeException re) {
            return new InternalServerError(re.getMessage(), re);
        }
        return backend.classify(cause).orElseGet(() -> new InternalServerError(message, cause));
    }
}
