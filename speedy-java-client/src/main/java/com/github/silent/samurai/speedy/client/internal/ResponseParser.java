package com.github.silent.samurai.speedy.client.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.exception.*;
import com.github.silent.samurai.speedy.client.format.JsonFormat;
import com.github.silent.samurai.speedy.client.format.SpeedyFormat;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;

/**
 * Central point for parsing server responses into domain types.
 * Enforces the client-server envelope contract (via the configured
 * {@link SpeedyFormat}) and routes errors to typed exceptions.
 */
public class ResponseParser {

    private final ObjectMapper mapper;
    private final SpeedyFormat format;

    public ResponseParser(ObjectMapper mapper) {
        this(mapper, new JsonFormat(mapper));
    }

    public ResponseParser(ObjectMapper mapper, SpeedyFormat format) {
        this.mapper = mapper;
        this.format = format;
    }

    /**
     * Parses an entity list response (create, get, update, delete, query).
     * Expects {@code {"payload": [...], "pageIndex": N, "pageSize": M, "totalCount": T, "totalPages": P}}.
     *
     * @param response the raw HTTP response
     * @return a SpeedyResult with typed access to the payload
     * @throws SpeedyException if the response status is not 2xx or JSON is malformed
     */
    public SpeedyResult parseEntityResponse(SpeedyRawResponse response) {
        if (!response.is2xx()) {
            throw parseError(response);
        }
        String body = response.body();
        if (body == null || body.isEmpty()) {
            return new SpeedyResult(mapper.createArrayNode(), 0, 0, 0, 0, mapper);
        }
        JsonNode root = format.read(body);
        JsonNode payload = root.has("payload") ? root.get("payload") : mapper.createArrayNode();
        if (payload == null || payload.isNull() || !payload.isArray()) {
            payload = mapper.createArrayNode();
        }
        int pageIndex = root.has("pageIndex") ? root.get("pageIndex").asInt(0) : 0;
        int pageSize = root.has("pageSize") ? root.get("pageSize").asInt(0) : 0;
        long totalCount = root.has("totalCount") ? root.get("totalCount").asLong(0) : 0;
        int totalPages = root.has("totalPages") ? root.get("totalPages").asInt(0) : 0;
        return new SpeedyResult(payload, pageIndex, pageSize, totalCount, totalPages, mapper);
    }

    /**
     * Parses a count response.
     * Expects {@code {"count": N}}.
     *
     * @param response the raw HTTP response
     * @return the count value
     * @throws SpeedyException if the response status is not 2xx or JSON is malformed
     */
    public long parseCountResponse(SpeedyRawResponse response) {
        if (!response.is2xx()) {
            throw parseError(response);
        }
        String body = response.body();
        if (body == null || body.isEmpty()) {
            return 0;
        }
        JsonNode root = format.read(body);
        return root.has("count") ? root.get("count").asLong(0) : 0;
    }

    /**
     * Parses an error response into a typed SpeedyException subclass.
     * Expects {@code {"status": N, "message": "...", "timestamp": "..."}}.
     *
     * @param response the raw HTTP response (non-2xx)
     * @return the typed exception (caller should throw it)
     */
    public SpeedyException parseError(SpeedyRawResponse response) {
        int statusCode = response.statusCode();
        String serverMessage = null;
        String timestamp = null;
        String body = response.body();

        if (body != null && !body.isEmpty()) {
            JsonNode root = tryReadErrorBody(body);
            if (root != null) {
                serverMessage = root.has("message") ? root.get("message").asText() : null;
                timestamp = root.has("timestamp") ? root.get("timestamp").asText() : null;
            } else {
                serverMessage = body;
            }
        }

        switch (statusCode / 100) {
            case 4:
                if (statusCode == 400) {
                    return new SpeedyBadRequestException(serverMessage, timestamp, body);
                }
                if (statusCode == 404) {
                    return new SpeedyNotFoundException(serverMessage, timestamp, body);
                }
                return new SpeedyException(statusCode, serverMessage, timestamp, body);
            case 5:
                return new SpeedyServerException(statusCode, serverMessage, timestamp, body);
            default:
                return new SpeedyException(statusCode, serverMessage, timestamp, body);
        }
    }

    /**
     * Errors thrown before the server completes content negotiation are written
     * with its JSON baseline serializer, so fall back to JSON when the
     * configured format can't parse the body.
     */
    private JsonNode tryReadErrorBody(String body) {
        try {
            return format.read(body);
        } catch (RuntimeException ignored) {
            // fall through to JSON baseline
        }
        try {
            return mapper.readTree(body);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }
}
