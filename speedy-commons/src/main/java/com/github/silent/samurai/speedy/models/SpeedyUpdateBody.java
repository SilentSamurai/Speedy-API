package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.interfaces.request.BulkRequestBody;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/// Request body for PUT/PATCH /{Entity}/$update operations.
///
/// Carries the list of (entity, primary key) pairs to update. Built by IRequestBodyParser
/// implementations from the raw HTTP body bytes.
@Getter
@Builder
public class SpeedyUpdateBody implements BulkRequestBody {

    /// The (entity, primary key) pairs to update, one per request element.
    private final List<Item> items;

    @Override
    public SpeedyRequestType getType() {
        return SpeedyRequestType.UPDATE;
    }

    @Override
    public int itemCount() {
        return items == null ? 0 : items.size();
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
