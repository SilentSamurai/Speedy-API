package com.github.silent.samurai.speedy.interfaces.query.backend;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

/// Write/mutation half of the backend port — the write-side analogue of
/// {@link com.github.silent.samurai.speedy.interfaces.SpeedyResponseWriter}.
///
/// The format-agnostic {@code SpeedyToRecord} walker (in speedy-core)
/// has already resolved each entity into a column {@link SpeedyEntity} (running application-side key
/// generation, skipping empty values, and flattening associations to their FK column value); the
/// port keeps the domain {@link com.github.silent.samurai.speedy.interfaces.SpeedyValue} as the
/// boundary currency and performs the value-to-column conversion itself (via its own backend
/// {@code Converter}) — leaf codec lives in the port, mirroring {@code SpeedyResponseWriter.writeLeaf}.
///
/// {@code columns} is a {@link SpeedyEntity} carrying the table metadata and the set fields whose
/// backing columns should be written (for an association field, the foreign key's value).
public interface RowWriter {

    void insert(SpeedyEntity columns) throws SpeedyHttpException;

    void update(SpeedyEntityKey pk, SpeedyEntity columns) throws SpeedyHttpException;

    /// Deletes the single row identified by {@code key}; the core owns the multi-row loop.
    void delete(SpeedyEntityKey key) throws SpeedyHttpException;
}
