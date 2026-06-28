package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import lombok.Builder;
import lombok.Getter;

/// Request body for PUT/PATCH /{Entity}/$update operations.
///
/// Carries the entity with fields to update and the primary key
/// identifying the target record. Built by IRequestBodyParser
/// implementations from the raw HTTP body bytes.
@Getter
@Builder
public class SpeedyUpdateBody implements SpeedyBody {

    /// The entity containing the fields to update.
    private final SpeedyEntity entity;

    /// The primary key identifying the record to update.
    private final SpeedyEntityKey pk;

    @Override
    public SpeedyRequestType getType() {
        return SpeedyRequestType.UPDATE;
    }
}
