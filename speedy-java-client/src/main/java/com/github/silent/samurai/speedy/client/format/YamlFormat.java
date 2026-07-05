package com.github.silent.samurai.speedy.client.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.silent.samurai.speedy.client.exception.SpeedyDeserializationException;

/**
 * YAML wire format, matching the server's {@code speedy-yaml-io} provider
 * ({@code application/yaml}).
 *
 * <p>Requires {@code com.fasterxml.jackson.dataformat:jackson-dataformat-yaml}
 * on the classpath (declared {@code <optional>} by this module — add it to your
 * own build to use this format).
 *
 * <p>YAML's Jackson backend shares the JSON tree model, so request trees
 * serialize directly and response bodies parse straight into the standard
 * envelope shape.
 */
public final class YamlFormat implements SpeedyFormat {

    public static final String CONTENT_TYPE = "application/yaml;charset=UTF-8";

    private final YAMLMapper yamlMapper = new YAMLMapper();

    @Override
    public String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public String write(JsonNode tree) {
        try {
            return yamlMapper.writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    @Override
    public JsonNode read(String body) {
        try {
            return yamlMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new SpeedyDeserializationException("Failed to parse YAML response: " + e.getMessage(), e);
        }
    }
}
