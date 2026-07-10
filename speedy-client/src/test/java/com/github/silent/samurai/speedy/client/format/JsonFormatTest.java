package com.github.silent.samurai.speedy.client.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.client.exception.SpeedyDeserializationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonFormatTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFormat format = new JsonFormat(mapper);

    @Test
    void contentTypeShouldBeJson() {
        assertEquals("application/json;charset=UTF-8", format.contentType());
    }

    @Test
    void treeShouldRoundTrip() {
        ObjectNode tree = mapper.createObjectNode();
        tree.put("name", "Widget");

        JsonNode parsed = format.read(format.write(tree));

        assertEquals("Widget", parsed.get("name").asText());
    }

    @Test
    void malformedJsonShouldThrowDeserializationException() {
        assertThrows(SpeedyDeserializationException.class, () -> format.read("not-json{"));
    }
}
