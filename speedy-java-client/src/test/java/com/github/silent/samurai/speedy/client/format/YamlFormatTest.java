package com.github.silent.samurai.speedy.client.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.exception.SpeedyDeserializationException;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlFormatTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final YamlFormat format = new YamlFormat();

    @Test
    void requestTreeShouldRoundTrip() {
        ObjectNode entity = mapper.createObjectNode();
        entity.put("name", "Widget");
        entity.put("cost", 12);
        ArrayNode body = mapper.createArrayNode();
        body.add(entity);

        JsonNode parsed = format.read(format.write(body));

        assertTrue(parsed.isArray());
        assertEquals("Widget", parsed.get(0).get("name").asText());
        assertEquals(12, parsed.get(0).get("cost").asInt());
    }

    @Test
    void envelopeShouldParseThroughResponseParser() {
        ResponseParser parser = new ResponseParser(mapper, format);
        String yaml = "payload:\n"
                + "- id: 1\n"
                + "  name: a\n"
                + "- id: 2\n"
                + "  name: b\n"
                + "pageIndex: 0\n"
                + "pageSize: 10\n"
                + "totalCount: 2\n"
                + "totalPages: 1\n";

        SpeedyResult result = parser.parseEntityResponse(new SpeedyRawResponse(200, Map.of(), yaml));

        assertEquals(2, result.size());
        assertEquals(2, result.totalCount());
        assertEquals("a", result.raw().get(0).get("name").asText());
    }

    @Test
    void countShouldParse() {
        ResponseParser parser = new ResponseParser(mapper, format);
        assertEquals(42, parser.parseCountResponse(new SpeedyRawResponse(200, Map.of(), "count: 42\n")));
    }

    @Test
    void jsonBodyIsValidYaml() {
        // JSON is a YAML subset — error fallback keeps working without special-casing.
        JsonNode root = format.read("{\"message\":\"bad\",\"timestamp\":\"now\"}");
        assertEquals("bad", root.get("message").asText());
    }

    @Test
    void malformedYamlShouldThrowDeserializationException() {
        assertThrows(SpeedyDeserializationException.class, () -> format.read("a: [unclosed"));
    }
}
