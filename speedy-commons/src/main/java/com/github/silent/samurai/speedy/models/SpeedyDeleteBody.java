package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.interfaces.request.BulkRequestBody;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/// Request body for DELETE /{Entity}/$delete operations.
///
/// Carries the list of primary keys to delete. Built by IRequestBodyParser
/// implementations from the raw HTTP body bytes.
@Getter
@Builder
public class SpeedyDeleteBody implements BulkRequestBody {

    /// The list of primary keys identifying records to delete.
    private final List<SpeedyEntityKey> keys;

    @Override
    public SpeedyRequestType getType() {
        return SpeedyRequestType.DELETE;
    }

    @Override
    public int itemCount() {
        return keys == null ? 0 : keys.size();
    }
}
