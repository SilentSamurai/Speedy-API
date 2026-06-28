package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.util.List;

/// Write/mutation half of the backend port — the write-side analogue of
/// {@link com.github.silent.samurai.speedy.interfaces.SpeedyResponseWriter}.
///
/// The format-agnostic {@code SpeedyToRecord} walker (in speedy-core)
/// has already resolved each entity into a column {@link SpeedyEntity} (running application-side key
/// generation, skipping empty values, and flattening associations to their FK column value); the
/// port keeps the domain {@link com.github.silent.samurai.speedy.interfaces.SpeedyValue} as the
/// boundary currency and performs the value-to-column conversion itself (via its own backend
/// {@code TypeConverter}) — leaf codec lives in the port, mirroring {@code SpeedyResponseWriter.writeLeaf}.
///
/// {@code columns} is a {@link SpeedyEntity} carrying the table metadata and the set fields whose
/// backing columns should be written (for an association field, the foreign key's value).
public interface RowWriter {

    /// Inserts all entities, writing any database-assigned key back onto each entity in place so the
    /// caller can refetch by primary key. The backend owns batching (e.g. a single JDBC round-trip).
    void insert(List<SpeedyEntity> entities) throws SpeedyHttpException;

    void update(SpeedyEntityKey pk, SpeedyEntity columns) throws SpeedyHttpException;

    /// Deletes every row identified by {@code keys} (single-key {@code IN} or composite-key {@code OR}
    /// is the backend's concern). Returns without effect for empty input.
    void deleteByKeys(List<SpeedyEntityKey> keys) throws SpeedyHttpException;
}
