package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.interfaces.request.BulkRequestBody;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/// Request body for POST /{Entity}/$create operations.
///
/// Carries the list of entities to create and the transaction mode
/// (BATCH or PER_ENTITY) for the operation. Built by IRequestBodyParser
/// implementations from the raw HTTP body bytes.
@Getter
@Builder
public class SpeedyCreateBody implements BulkRequestBody {

    /// The list of entity instances to create.
    private final List<SpeedyEntity> entities;

    /// Transaction mode for the create operation.
    private final TransactionMode mode;

    @Override
    public SpeedyRequestType getType() {
        return SpeedyRequestType.CREATE;
    }

    @Override
    public int itemCount() {
        return entities == null ? 0 : entities.size();
    }
}
