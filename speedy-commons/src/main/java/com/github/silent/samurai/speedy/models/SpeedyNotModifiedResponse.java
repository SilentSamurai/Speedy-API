package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyResponseType;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Bodiless 304 response emitted when a GET's If-None-Match matches the current ETag. Carries
/// only the ETag header; the serializer commits status and headers directly to the servlet
/// response for this type, without invoking the entity/token writer pipeline.
@Getter
@Builder
public class SpeedyNotModifiedResponse implements SpeedyResponse {

    @Builder.Default
    private final int status = 304;

    @Builder.Default
    private final Map<String, String> headers = new LinkedHashMap<>();

    @Override
    public SpeedyResponseType getType() {
        return SpeedyResponseType.NOT_MODIFIED;
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
