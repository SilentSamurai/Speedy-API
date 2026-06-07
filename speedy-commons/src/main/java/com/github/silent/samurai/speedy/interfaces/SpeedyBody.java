package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;

/// The payload of a Speedy API request, varying by operation type.
///
/// Implementations carry the operation-specific data:
/// SpeedyQuery for read operations, SpeedyCreateBody for create,
/// SpeedyUpdateBody for update, and SpeedyDeleteBody for delete.
public interface SpeedyBody {

    /// The type of request this body represents.
    SpeedyRequestType getType();
}
