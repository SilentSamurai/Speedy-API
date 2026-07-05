package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Registry of the wire formats a Speedy endpoint can negotiate, driving the
/// format-parameterized MockMvc suites ({@code SpeedyIo*Test}).
///
/// Request bodies are built as format-agnostic Jackson trees (identical to the JSON tests)
/// and serialized here; responses are parsed back into a {@link JsonNode} so tests can assert
/// with JsonPointer regardless of the wire format. The only per-format state is the negotiated
/// media type and the Jackson {@link ObjectMapper} — {@link YAMLMapper} and the plain JSON
/// {@code ObjectMapper} emit the same tree model, so every format shares one code path.
///
/// Add a format by adding a constant (and, for the server side, a matching
/// {@code ISpeedyIoProvider}); e.g. {@code XML("application/xml", new XmlMapper())} once
/// {@code speedy-xml-io} exists.
public enum IoFormat {

    JSON("application/json;charset=UTF-8", new ObjectMapper()),
    YAML("application/yaml;charset=UTF-8", new YAMLMapper());

    /// The media type negotiated for both request ({@code Content-Type}) and response
    /// ({@code Accept}) in the suites.
    public final String media;

    private final ObjectMapper mapper;

    IoFormat(String media, ObjectMapper mapper) {
        this.media = media;
        this.mapper = mapper;
    }

    /// Serializes a request-body tree (built the same way as the JSON tests) to this format.
    public String write(JsonNode body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize " + this + " body", e);
        }
    }

    /// Parses a response body of this format into a {@link JsonNode} for JsonPointer assertions.
    public JsonNode tree(MvcResult result) {
        try {
            return mapper.readTree(result.getResponse().getContentAsString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse " + this + " response", e);
        }
    }

    /// Asserts the response was rendered in this format — a silent fall-back to another format
    /// (e.g. JSON) fails here.
    public void assertNegotiated(MvcResult result) {
        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType, "response had no Content-Type");
        assertTrue(contentType.contains(media),
                "expected " + media + " content type but was " + contentType);
    }
}
