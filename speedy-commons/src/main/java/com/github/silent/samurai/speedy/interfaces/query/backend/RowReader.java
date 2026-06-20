package com.github.silent.samurai.speedy.interfaces.query.backend;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/// Read/fetch half of the backend port. The format-agnostic
/// {@link com.github.silent.samurai.speedy.query.walker.WalkingQueryProcessor} drives the
/// orchestration and the {@link com.github.silent.samurai.speedy.query.walker.RecordToSpeedy}
/// walker assembles the entity tree; this port only *fetches rows* — the backend owns statement
/// building, execution, and value decoding.
///
/// A fetched row is returned as a *flat* {@link SpeedyEntity}: scalar fields decoded to their
/// {@link com.github.silent.samurai.speedy.interfaces.SpeedyValue}, and each association field
/// carrying its foreign-key value (decoded with the associated field's type). The
/// {@link com.github.silent.samurai.speedy.query.walker.RecordToSpeedy} walker then resolves those
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

    /// The single related row reached by following the foreign key of {@code association} from
    /// {@code parentRow} (used during {@code $expand}); empty when the FK is null or unresolved.
    Optional<SpeedyEntity> selectByFk(FieldMetadata association, SpeedyEntity parentRow) throws SpeedyHttpException;
}
