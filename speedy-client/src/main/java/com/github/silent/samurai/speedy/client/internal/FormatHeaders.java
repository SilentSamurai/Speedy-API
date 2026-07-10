package com.github.silent.samurai.speedy.client.internal;

import com.github.silent.samurai.speedy.client.format.SpeedyFormat;

import java.util.List;
import java.util.Map;

/**
 * Builds the negotiation headers for a request from the client's wire format:
 * {@code Accept} always, {@code Content-Type} only when a body is sent.
 */
public final class FormatHeaders {

    private FormatHeaders() {
    }

    public static Map<String, List<String>> forBody(SpeedyFormat format) {
        return Map.of(
                "Accept", List.of(format.contentType()),
                "Content-Type", List.of(format.contentType()));
    }

    public static Map<String, List<String>> acceptOnly(SpeedyFormat format) {
        return Map.of("Accept", List.of(format.contentType()));
    }
}
