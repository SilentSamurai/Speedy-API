package com.github.silent.samurai.speedy.client.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.exception.*;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;

/**
 * Central point for parsing server responses into domain types.
 * Enforces the client-server JSON contract and routes errors to typed exceptions.
 */
public class ResponseParser {

    private final ObjectMapper mapper;

    public ResponseParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Parses an entity list response (create, get, update, delete, query).
     * Expects {@code {"payload": [...], "pageIndex": N, "pageSize": M}}.
     *
     * @param response the raw HTTP response
     * @return a SpeedyResult with typed access to the payload
     * @throws SpeedyException if the response status is not 2xx or JSON is malformed
     */
    public SpeedyResult parseEntityResponse(SpeedyRawResponse response) {
        if (!response.is2xx()) {
            throw parseError(response);
        }
        try {
            String body = response.body();
            if (body == null || body.isEmpty()) {
                return new SpeedyResult(mapper.createArrayNode(), 0, 0, mapper);
            }
            JsonNode root = mapper.readTree(body);
            JsonNode payload = root.has("payload") ? root.get("payload") : mapper.createArrayNode();
            if (payload == null || payload.isNull() || !payload.isArray()) {
                payload = mapper.createArrayNode();
            }
            int pageIndex = root.has("pageIndex") ? root.get("pageIndex").asInt(0) : 0;
            int pageSize = root.has("pageSize") ? root.get("pageSize").asInt(0) : 0;
            return new SpeedyResult(payload, pageIndex, pageSize, mapper);
        } catch (JsonProcessingException e) {
            throw new SpeedyDeserializationException("Failed to parse entity response: " + e.getMessage(), e);
        }
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
        try {
            String body = response.body();
            if (body == null || body.isEmpty()) {
                return 0;
            }
            JsonNode root = mapper.readTree(body);
            return root.has("count") ? root.get("count").asLong(0) : 0;
        } catch (JsonProcessingException e) {
            throw new SpeedyDeserializationException("Failed to parse count response: " + e.getMessage(), e);
        }
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
            try {
                JsonNode root = mapper.readTree(body);
                serverMessage = root.has("message") ? root.get("message").asText() : null;
                timestamp = root.has("timestamp") ? root.get("timestamp").asText() : null;
            } catch (JsonProcessingException e) {
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
}
