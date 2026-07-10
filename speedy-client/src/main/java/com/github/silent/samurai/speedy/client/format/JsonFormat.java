package com.github.silent.samurai.speedy.client.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.client.exception.SpeedyDeserializationException;

/**
 * Default JSON wire format. Wraps the client's single {@link ObjectMapper} so
 * user-supplied mapper configuration applies to the wire representation.
 */
public final class JsonFormat implements SpeedyFormat {

    public static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    private final ObjectMapper mapper;

    public JsonFormat(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public String write(JsonNode tree) {
        try {
            return mapper.writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    @Override
    public JsonNode read(String body) {
        try {
            return mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new SpeedyDeserializationException("Failed to parse JSON response: " + e.getMessage(), e);
        }
    }
}
