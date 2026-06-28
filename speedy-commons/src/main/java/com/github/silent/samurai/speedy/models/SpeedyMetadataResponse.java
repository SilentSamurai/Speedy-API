package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyResponseType;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Server-level response describing the whole metamodel (the `$metadata` endpoint).
///
/// Not tied to any single entity; the serializer renders the supplied
/// {@link MetaModel} in its own content type.
@Getter
@Builder
public class SpeedyMetadataResponse implements SpeedyResponse {

    /// The metamodel to describe in the response.
    private final MetaModel metaModel;

    /// HTTP status code for the response.
    @Builder.Default
    private final int status = 200;

    /// HTTP response headers as name-value pairs.
    @Builder.Default
    private final Map<String, String> headers = new LinkedHashMap<>();

    @Override
    public SpeedyResponseType getType() {
        return SpeedyResponseType.METADATA;
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
