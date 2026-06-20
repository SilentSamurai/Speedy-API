package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyResponseType;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Server-level error response — not tied to any entity.
///
/// Carries the HTTP status and a human-readable message; the serializer
/// stamps the timestamp at write time in its own content type.
@Getter
@Builder
public class SpeedyErrorResponse implements SpeedyResponse {

    /// The HTTP status code describing the failure.
    private final int status;

    /// Human-readable description of the failure.
    private final String message;

    /// HTTP response headers as name-value pairs.
    @Builder.Default
    private final Map<String, String> headers = new LinkedHashMap<>();

    @Override
    public SpeedyResponseType getType() {
        return SpeedyResponseType.ERROR;
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
