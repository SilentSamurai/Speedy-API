package com.github.silent.samurai.speedy.enums;

/// The write operations for which an entity can opt into bulk (multi-element) request bodies.
///
/// Used by {@code @SpeedyBulk} to selectively enable bulk on any subset of the write
/// operations. {@link #ALL} is a convenience meaning "every operation". Unlike
/// {@link ActionType} there is no {@code READ}: reads are already inherently multi-row
/// via the {@code $query} endpoint and are never gated by bulk.
///
/// {@link #UPDATE} governs PATCH ({@code $update}); {@link #REPLACE} governs PUT — they are
/// controlled independently because they are distinct write chains.
public enum BulkOperation {
    CREATE,
    UPDATE,
    REPLACE,
    DELETE,
    ALL
}
