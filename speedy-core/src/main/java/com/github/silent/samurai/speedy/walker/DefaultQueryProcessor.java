package com.github.silent.samurai.speedy.walker;

import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.query.backend.SpeedyBackend;
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
import java.util.Set;

/// Format-agnostic {@link QueryProcessor}: owns CRUD orchestration (query execution, the
/// create/delete refetch-by-primary-key loops, update, exists, transaction handling, and native
/// exception mapping) while delegating every backend-specific operation to a {@link SpeedyBackend}
/// port. The persistence-side mirror of
/// {@code com.github.silent.samurai.speedy.serialization.WalkingResponseSerializer}: the only
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
            List<SpeedyEntity> rows = backend.select(speedyQuery);
            List<SpeedyEntity> list = new ArrayList<>(rows.size());
            for (SpeedyEntity row : rows) {
                list.add(recordToSpeedy.fromRow(row, speedyQuery.getFrom(), speedyQuery.getExpand()));
            }
            return list;
        } catch (Exception e) {
            throw wrap("Invalid Request", e);
        }
    }

    @Override
    public QueryResult executeManyWithCount(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            BigInteger totalCount = backend.count(speedyQuery);
            List<SpeedyEntity> rows = backend.select(speedyQuery);
            List<SpeedyEntity> list = new ArrayList<>(rows.size());
            for (SpeedyEntity row : rows) {
                list.add(recordToSpeedy.fromRow(row, speedyQuery.getFrom(), speedyQuery.getExpand()));
            }
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

            List<SpeedyEntity> entityList = new ArrayList<>(entities.size());

            if (!entities.isEmpty()) {
                List<SpeedyEntityKey> keys = new ArrayList<>(entities.size());
                for (SpeedyEntity entity : entities) {
                    keys.add(SpeedyEntityUtil.toEntityKey(entity));
                }
                EntityMetadata entityMetadata = entities.get(0).getMetadata();
                Map<SpeedyEntityKey, SpeedyEntity> entityMap = refetchByKeys(keys, entityMetadata);

                for (SpeedyEntityKey key : keys) {
                    SpeedyEntity entity = entityMap.get(key);
                    if (entity != null) {
                        entityList.add(entity);
                    }
                }

                if (entityList.size() != keys.size()) {
                    LOGGER.warn("selectByKeys returned {} results for {} keys for entity '{}'",
                            entityList.size(), keys.size(), entityMetadata.getName());
                }
            }

            return entityList;
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
            List<SpeedyEntity> entities;
            if (pks.isEmpty()) {
                entities = new ArrayList<>();
            } else {
                EntityMetadata entityMetadata = pks.get(0).getMetadata();
                Map<SpeedyEntityKey, SpeedyEntity> entityMap = refetchByKeys(pks, entityMetadata);

                entities = new ArrayList<>(pks.size());
                for (SpeedyEntityKey key : pks) {
                    SpeedyEntity entity = entityMap.get(key);
                    if (entity != null) {
                        entities.add(entity);
                    }
                }

                if (entities.size() != pks.size()) {
                    LOGGER.warn("selectByKeys returned {} results for {} keys for entity '{}'",
                            entities.size(), pks.size(), entityMetadata.getName());
                }
            }

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

    /// Refetches rows for the given keys and indexes the rebuilt entities by their primary key.
    private Map<SpeedyEntityKey, SpeedyEntity> refetchByKeys(List<SpeedyEntityKey> keys, EntityMetadata entityMetadata)
            throws SpeedyHttpException {
        List<SpeedyEntity> rows = backend.selectByKeys(keys);
        Map<SpeedyEntityKey, SpeedyEntity> entityMap = new HashMap<>();
        for (SpeedyEntity row : rows) {
            SpeedyEntity entity = recordToSpeedy.fromRow(row, entityMetadata, Set.of());
            entityMap.put(SpeedyEntityUtil.toEntityKey(entity), entity);
        }
        return entityMap;
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
