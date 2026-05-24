package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Typed wrapper around successful entity responses from the Speedy API.
 *
 * <p>Holds the raw {@code payload} JSON array and pagination metadata,
 * with convenience methods for typed deserialization.
 */
public class SpeedyResult {

    private final JsonNode payload;
    private final int pageIndex;
    private final int pageSize;
    private final ObjectMapper mapper;

    public SpeedyResult(JsonNode payload, int pageIndex, int pageSize, ObjectMapper mapper) {
        this.payload = payload != null ? payload : mapper.createArrayNode();
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.mapper = mapper;
    }

    /**
     * Deserializes all entities in the payload as a typed list.
     */
    public <T> List<T> list(Class<T> type) {
        try {
            return mapper.readValue(
                    mapper.treeAsTokens(payload),
                    mapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize response payload", e);
        }
    }

    /**
     * Deserializes the first entity in the payload, or null if empty.
     */
    public <T> T first(Class<T> type) {
        if (payload.isEmpty()) {
            return null;
        }
        try {
            return mapper.treeToValue(payload.get(0), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize first entity", e);
        }
    }

    /**
     * Returns the first entity as an {@link Optional}, or empty if no entities.
     */
    public <T> Optional<T> firstOptional(Class<T> type) {
        return Optional.ofNullable(first(type));
    }

    /**
     * Returns the raw payload {@link JsonNode} (JSON array).
     */
    public JsonNode raw() {
        return payload;
    }

    /**
     * Returns the first element of the payload as raw {@link JsonNode}, or null if empty.
     */
    public JsonNode firstRaw() {
        return payload.isEmpty() ? null : payload.get(0);
    }

    public int pageIndex() {
        return pageIndex;
    }

    public int pageSize() {
        return pageSize;
    }

    public boolean isEmpty() {
        return payload.isEmpty();
    }

    public int size() {
        return payload.size();
    }
}
