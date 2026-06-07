package com.github.silent.samurai.speedy.enums;

/// Classifies Speedy API requests by operation type.
///
/// Used by OperationResolverHandler to determine the operation
/// and by SwitchHandler to dispatch to the appropriate handler.
public enum SpeedyRequestType {

    /// GET /{Entity} — list entities with URL query params.
    GET_LIST,

    /// POST /{Entity}/$query — advanced query with JSON body.
    QUERY,

    /// POST /{Entity}/$create — bulk create from JSON array.
    CREATE,

    /// PUT/PATCH /{Entity}/$update — update single entity by PK.
    UPDATE,

    /// DELETE /{Entity}/$delete — bulk delete by PK array.
    DELETE
}
