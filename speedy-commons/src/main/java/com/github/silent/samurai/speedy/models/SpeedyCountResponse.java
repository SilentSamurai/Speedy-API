package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyResponseType;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Response containing only the count of entities matching a query.
///
/// Produced when `$count=true` is specified in a GET or $query
/// request instead of returning full entity data.
@Getter
@Builder
public class SpeedyCountResponse implements SpeedyResponse {

    /// The total number of entities matching the query criteria.
    private final BigInteger count;

    /// HTTP status code for the response.
    @Builder.Default
    private final int status = 200;

    /// HTTP response headers as name-value pairs.
    @Builder.Default
    private final Map<String, String> headers = new LinkedHashMap<>();

    @Override
    public SpeedyResponseType getType() {
        return SpeedyResponseType.COUNT;
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
