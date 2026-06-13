package com.github.silent.samurai.speedy.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// Immutable wrapper for HTTP request headers, enabling storage in SpeedyContext.
public class SpeedyHeaders {

    private final Map<String, String> headers;

    public SpeedyHeaders(Map<String, String> headers) {
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
    }

    public String get(String name) {
        return headers.get(name);
    }

    public Map<String, String> asMap() {
        return headers;
    }
}
