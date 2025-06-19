package com.github.silent.samurai.speedy.api.client.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class for JSON operations in the Speedy Java Client.
 * This is a library-agnostic utility that only handles JSON serialization/deserialization.
 */
public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
    }

    /**
     * Get the ObjectMapper instance.
     *
     * @return the configured ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Convert an object to JSON string.
     *
     * @param value the object to convert
     * @return JSON string representation
     * @throws JsonProcessingException if conversion fails
     */
    public static String toJson(Object value) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(value);
    }

    /**
     * Convert a JsonNode to a specific type.
     *
     * @param <T>      the target type
     * @param jsonNode the JsonNode to convert
     * @param type     the target class
     * @return the converted object
     * @throws JsonProcessingException if conversion fails
     */
    public static <T> T fromJson(JsonNode jsonNode, Class<T> type) throws JsonProcessingException {
        return OBJECT_MAPPER.treeToValue(jsonNode, type);
    }

    /**
     * Convert a JSON string to a specific type.
     *
     * @param <T>  the target type
     * @param json the JSON string
     * @param type the target class
     * @return the converted object
     * @throws JsonProcessingException if conversion fails
     */
    public static <T> T fromJson(String json, Class<T> type) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, type);
    }

    /**
     * Parse a JSON string to JsonNode.
     *
     * @param json the JSON string
     * @return the JsonNode
     * @throws JsonProcessingException if parsing fails
     */
    public static JsonNode parseJson(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(json);
    }
} 