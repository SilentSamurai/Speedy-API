package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/// Request body for PUT/PATCH /{Entity}/$update operations.
///
/// Carries the list of (entity, primary key) pairs to update and the transaction mode
/// (BATCH or PER_ENTITY) for the operation. Built by IRequestBodyParser
/// implementations from the raw HTTP body bytes.
@Getter
@Builder
public class SpeedyUpdateBody implements SpeedyBody {

    /// The (entity, primary key) pairs to update, one per request element.
    private final List<Item> items;

    /// Transaction mode for the update operation.
    private final TransactionMode mode;

    @Override
    public SpeedyRequestType getType() {
        return SpeedyRequestType.UPDATE;
    }

    /// A single update target: the fields to write, paired with the primary key
    /// identifying the record to write them to.
    @Getter
    @Builder
    public static class Item {

        /// The entity containing the fields to update.
        private final SpeedyEntity entity;

        /// The primary key identifying the record to update.
        private final SpeedyEntityKey pk;
    }
}
