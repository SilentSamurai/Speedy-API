package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyResponseType;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Response containing the results of a batch create or delete operation.
///
/// Includes both successfully processed entities and partial failures with
/// individual error details.
@Getter
@Builder
public class SpeedyBatchResponse implements SpeedyResponse {

    /// List of entities that were successfully processed.
    @Builder.Default
    private final List<SpeedyEntity> succeeded = Collections.emptyList();

    /// List of individual failures with details (index, message, cause).
    @Builder.Default
    private final List<SpeedyPartialFailure> failed = Collections.emptyList();

    /// Zero-based page index (always 0 for batch operations).
    private final int pageIndex;

    /// HTTP status code. 200 if all succeeded, 207 for partial success, 400 if all failed.
    @Builder.Default
    private final int status = 200;

    /// HTTP response headers as name-value pairs.
    @Builder.Default
    private final Map<String, String> headers = new LinkedHashMap<>();

    @Override
    public SpeedyResponseType getType() {
        return SpeedyResponseType.BATCH_RESULT;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }
}
