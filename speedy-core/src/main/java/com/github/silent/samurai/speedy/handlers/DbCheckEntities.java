package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/// Persisted target rows loaded by a database-check handler.
///
/// Stored in {@code SpeedyContext} by {@link ExistsInDbCheckHandler}, this gives downstream
/// handlers (update, replace, and delete) the same preflighted view of the target rows without
/// repeating database reads.
public record DbCheckEntities(Map<SpeedyEntityKey, SpeedyEntity> byKey) {

    public DbCheckEntities {
        Objects.requireNonNull(byKey, "byKey must not be null");
        byKey = Collections.unmodifiableMap(new LinkedHashMap<>(byKey));
    }

    /// Returns the preflighted persisted row for a request key.
    public SpeedyEntity get(SpeedyEntityKey key) {
        SpeedyEntity entity = byKey.get(key);
        if (entity == null) {
            throw new IllegalStateException("No preflighted entity for key: " + key);
        }
        return entity;
    }
}
