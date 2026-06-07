package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.interfaces.SpeedyBody;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/// Request body for DELETE /{Entity}/$delete operations.
///
/// Carries the list of primary keys to delete and the transaction mode
/// (BATCH or PER_ENTITY) for the operation. Built by IRequestBodyParser
/// implementations from the raw HTTP body bytes.
@Getter
@Builder
public class SpeedyDeleteBody implements SpeedyBody {

    /// The list of primary keys identifying records to delete.
    private final List<SpeedyEntityKey> keys;

    /// Transaction mode for the delete operation.
    private final TransactionMode mode;

    @Override
    public SpeedyRequestType getType() {
        return SpeedyRequestType.DELETE;
    }
}
