package com.github.silent.samurai.speedy.interfaces.request;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;

/// The payload of a Speedy API request, varying by operation type.
///
/// Implementations carry the operation-specific data:
/// SpeedyQuery for read operations, SpeedyCreateBody for create,
/// SpeedyUpdateBody for update, and SpeedyDeleteBody for delete.
public interface SpeedyBody {

    /// The type of request this body represents.
    SpeedyRequestType getType();

    /// Returns a body that carries only the request type and no payload.
    /// Used for operations that have no request body (e.g. METADATA).
    static SpeedyBody empty(SpeedyRequestType type) {
        return () -> type;
    }
}
