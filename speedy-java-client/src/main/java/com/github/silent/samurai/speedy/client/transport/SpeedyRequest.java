package com.github.silent.samurai.speedy.client.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable value object representing an HTTP request to the Speedy API.
 *
 * <p>Mutation methods ({@link #withHeader}, {@link #withHeaders}) return new instances
 * — the original is unchanged. This enables safe interceptor chaining.
 */
public record SpeedyRequest(String method, String url, Map<String, List<String>> headers, String body) {

    public SpeedyRequest(String method, String url, Map<String, List<String>> headers, String body) {
        this.method = method;
        this.url = url;
        this.headers = headers == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(headers));
        this.body = body;
    }

    /**
     * Returns a new {@code SpeedyRequest} with the given header added.
     */
    public SpeedyRequest withHeader(String name, String value) {
        Map<String, List<String>> newHeaders = new HashMap<>(this.headers);
        newHeaders.put(name, List.of(value));
        return new SpeedyRequest(method, url, newHeaders, body);
    }

    /**
     * Returns a new {@code SpeedyRequest} with the given headers added.
     */
    public SpeedyRequest withHeaders(Map<String, String> additionalHeaders) {
        Map<String, List<String>> newHeaders = new HashMap<>(this.headers);
        for (Map.Entry<String, String> entry : additionalHeaders.entrySet()) {
            newHeaders.put(entry.getKey(), List.of(entry.getValue()));
        }
        return new SpeedyRequest(method, url, newHeaders, body);
    }
}
