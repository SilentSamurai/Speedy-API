package com.github.silent.samurai.speedy.interfaces.backend;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.math.BigInteger;
import java.util.List;

/// Read/fetch half of the backend port. The format-agnostic
/// {@code DefaultQueryProcessor} (in speedy-core) drives the
/// orchestration and the {@code RecordToSpeedy}
/// walker assembles the entity tree; this port only *fetches rows* — the backend owns statement
/// building, execution, and value decoding.
///
/// A fetched row is returned as a *flat* {@link SpeedyEntity}: scalar fields decoded to their
/// {@link com.github.silent.samurai.speedy.interfaces.SpeedyValue}, and each association field
/// carrying its foreign-key value (decoded with the associated field's type). The
/// {@code RecordToSpeedy} walker then resolves those
/// associations into nested entities ({@code $expand}) or keys-only references. The backend's native
/// row type (a jOOQ {@code Record}, a JDBC {@code ResultSet} row, a Mongo {@code Document}, …) never
/// crosses this boundary — {@link SpeedyEntity}/{@code SpeedyValue} are the only currency.
public interface RowReader {

    /// Rows matching the query (where/order/paging/projection are the backend's concern).
    List<SpeedyEntity> select(SpeedyQuery query) throws SpeedyHttpException;

    /// Count of rows matching the query's filter.
    BigInteger count(SpeedyQuery query) throws SpeedyHttpException;

    /// Rows for the given primary keys (single-key {@code IN} or composite-key {@code OR} is the
    /// backend's concern). Returns an empty list for empty input.
    List<SpeedyEntity> selectByKeys(List<SpeedyEntityKey> keys) throws SpeedyHttpException;

    /// Whether a row exists for the given primary key — a presence check that should avoid selecting
    /// and decoding the full row. Defaults to a {@link #selectByKeys} probe; backends should override
    /// with a native existence query (e.g. {@code SELECT 1 ... LIMIT 1}).
    default boolean existsByKey(SpeedyEntityKey key) throws SpeedyHttpException {
        return !selectByKeys(List.of(key)).isEmpty();
    }

    /// Rows of {@code association}'s target whose associated field matches any of {@code fkValues}
    /// — the batched foreign-key fetch that drives {@code $expand}. Returns an empty list for empty
    /// input, and may return fewer rows than keys (an unmatched key resolves to null).
    ///
    /// The walker calls this once per expansion level for the whole result set, so a backend should
    /// answer it with a single statement (an {@code IN} list or equivalent). Callers chunk the key
    /// list, so an implementation need not split it further.
    List<SpeedyEntity> selectByFks(FieldMetadata association, List<SpeedyValue> fkValues) throws SpeedyHttpException;
}
