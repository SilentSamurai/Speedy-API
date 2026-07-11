package com.github.silent.samurai.speedy.enums;

/// Classifies Speedy API requests by operation type.
///
/// Used by OperationResolverHandler to determine the operation
/// and by SpeedyFactory to dispatch to the appropriate engine method.
public enum SpeedyRequestType {

    /// GET /{Entity} — list entities with URL query params.
    GET_LIST,

    /// POST /{Entity}/$query — advanced query with JSON body.
    QUERY,

    /// POST /{Entity}/$create — bulk create from JSON array.
    CREATE,

    /// PATCH /{Entity}/$update — partial update of a single entity by PK
    /// (only supplied fields are written).
    UPDATE,

    /// PUT /{Entity}/$update — full replace of a single entity by PK
    /// (required fields enforced, omitted nullable fields reset to null).
    REPLACE,

    /// DELETE /{Entity}/$delete — bulk delete by PK array.
    DELETE,

    /// POST /{Entity}/$restore — restore (un-delete) soft-deleted rows by PK array.
    RESTORE,

    /// POST /{Entity}/$purge — permanently hard-delete rows by PK array.
    PURGE,

    /// GET / or GET /{Entity} — server/entity metadata description.
    METADATA
}
