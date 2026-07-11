package com.github.silent.samurai.speedy.models;

import java.util.HashMap;
import java.util.Map;

/// Immutable wrapper for HTTP request headers with case-insensitive lookup,
/// enabling storage in SpeedyContext.
public class SpeedyHeaders {

    private final Map<String, String> headers;

    public SpeedyHeaders(Map<String, String> headers) {
        Map<String, String> normalized = new HashMap<>();
        for (var entry : headers.entrySet()) {
            normalized.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        this.headers = Map.copyOf(normalized);
    }

    public String get(String name) {
        return headers.get(name.toLowerCase());
    }

    public Map<String, String> asMap() {
        return headers;
    }
}
