package com.github.silent.samurai.speedy.parser;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal URI decomposition used by Speedy's URL query language.
 *
 * <p>This intentionally accepts the relaxed URI forms supported by Speedy, including quoted
 * values and spaces, which {@link java.net.URI} rejects unless they are pre-encoded.</p>
 */
public final class SpeedyUriComponents {

    private final List<String> pathSegments;
    private final Map<String, List<String>> queryParameters;

    private SpeedyUriComponents(List<String> pathSegments, Map<String, List<String>> queryParameters) {
        this.pathSegments = List.copyOf(pathSegments);
        Map<String, List<String>> immutableParameters = new LinkedHashMap<>();
        queryParameters.forEach((key, values) -> immutableParameters.put(key, Collections.unmodifiableList(values)));
        this.queryParameters = Collections.unmodifiableMap(immutableParameters);
    }

    /**
     * Parses a still-percent-encoded URI: the input is URL-decoded once, then split.
     *
     * <p>Pass the raw request URI here <em>exactly once</em>. Callers that have already decoded the
     * URI must use {@link #parseDecoded(String)} instead — decoding twice corrupts any value that
     * legitimately contains a percent sign or an encoded delimiter.</p>
     */
    public static SpeedyUriComponents parse(String uri) {
        return parseDecoded(URLDecoder.decode(uri, StandardCharsets.UTF_8));
    }

    /**
     * Splits an already-decoded URI into path segments and query parameters without decoding again.
     * Decoding is split-then-* here, so an encoded {@code &}/{@code =} inside a value is treated as a
     * delimiter — matching the behaviour of the Spring {@code UriComponentsBuilder} this replaced.
     */
    static SpeedyUriComponents parseDecoded(String decoded) {
        int queryStart = decoded.indexOf('?');
        String path = queryStart >= 0 ? decoded.substring(0, queryStart) : decoded;
        String query = queryStart >= 0 ? decoded.substring(queryStart + 1) : "";

        List<String> segments = Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isEmpty())
                .toList();

        Map<String, List<String>> parameters = new LinkedHashMap<>();
        if (!query.isEmpty()) {
            for (String parameter : query.split("&", -1)) {
                if (parameter.isEmpty()) {
                    continue;
                }
                int equals = parameter.indexOf('=');
                String name = equals >= 0 ? parameter.substring(0, equals) : parameter;
                String value = equals >= 0 ? parameter.substring(equals + 1) : null;
                parameters.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
            }
        }

        return new SpeedyUriComponents(segments, parameters);
    }

    public List<String> getPathSegments() {
        return pathSegments;
    }

    public Map<String, List<String>> getQueryParameters() {
        return queryParameters;
    }

    public String getFirstQueryParameter(String name) {
        List<String> values = queryParameters.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
