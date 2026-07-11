package com.github.silent.samurai.speedy.interfaces.backend;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.SpeedyEntityUtil;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface QueryProcessor {

    BigInteger executeCount(SpeedyQuery query) throws SpeedyHttpException;

    List<SpeedyEntity> executeMany(SpeedyQuery query) throws SpeedyHttpException;

    default QueryResult executeManyWithCount(SpeedyQuery query) throws SpeedyHttpException {
        List<SpeedyEntity> entities = executeMany(query);
        BigInteger totalCount = executeCount(query);
        return new QueryResult(entities, totalCount);
    }

    boolean exists(SpeedyEntityKey entityKey) throws SpeedyHttpException;

    /// Returns the subset of {@code keys} that already exist in the database.
    /// Default implementation probes each key individually; backends should override
    /// with a single batched query (e.g. {@code SELECT key FROM ... WHERE key IN (...)}).
    default Set<SpeedyEntityKey> findExistingKeys(List<SpeedyEntityKey> keys) throws SpeedyHttpException {
        Set<SpeedyEntityKey> existing = new HashSet<>();
        for (SpeedyEntityKey key : keys) {
            if (exists(key)) {
                existing.add(key);
            }
        }
        return existing;
    }

    List<SpeedyEntity> create(List<SpeedyEntity> entities) throws SpeedyHttpException;

    SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException;

    /// Full-replace (PUT) counterpart of {@link #update}: the entity is the complete
    /// representation of the row, so omitted nullable non-key columns are reset to null.
    /// Default throws; {@code DefaultQueryProcessor} provides the real implementation.
    default SpeedyEntity replace(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        throw new UnsupportedOperationException(
                "replace not supported by this QueryProcessor"
        );
    }

    List<SpeedyEntity> delete(List<SpeedyEntityKey> entityKeys) throws SpeedyHttpException;

    /// Restores (un-deletes) soft-deleted rows by clearing their soft-delete marker. Each key must
    /// identify a currently soft-deleted row. Returns the restored (now live) rows.
    /// Default throws; {@code DefaultQueryProcessor} provides the real implementation.
    default List<SpeedyEntity> restore(List<SpeedyEntityKey> entityKeys) throws SpeedyHttpException {
        throw new UnsupportedOperationException(
                "restore not supported by this QueryProcessor"
        );
    }

    /// Permanently (hard) deletes rows by primary key, bypassing soft delete. Returns the removed
    /// rows (key-only). Default throws; {@code DefaultQueryProcessor} provides the real implementation.
    default List<SpeedyEntity> purge(List<SpeedyEntityKey> entityKeys) throws SpeedyHttpException {
        throw new UnsupportedOperationException(
                "purge not supported by this QueryProcessor"
        );
    }

    default void runInTransaction(Runnable block) {
        throw new UnsupportedOperationException(
                "runInTransaction not supported by this QueryProcessor"
        );
    }

}
