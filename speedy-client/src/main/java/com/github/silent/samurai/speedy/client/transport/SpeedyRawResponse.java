package com.github.silent.samurai.speedy.client.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable value object representing the raw HTTP response from the transport layer.
 *
 * <p>{@code body} may be null for 204 No Content or empty responses.
 * {@code statusCode} and {@code headers} are never null.
 */
public final class SpeedyRawResponse {

    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final String body;

    public SpeedyRawResponse(int statusCode, Map<String, List<String>> headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(headers));
        this.body = body;
    }

    public int statusCode() {
        return statusCode;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public String body() {
        return body;
    }

    public boolean is2xx() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean is4xx() {
        return statusCode >= 400 && statusCode < 500;
    }

    public boolean is5xx() {
        return statusCode >= 500 && statusCode < 600;
    }
}
