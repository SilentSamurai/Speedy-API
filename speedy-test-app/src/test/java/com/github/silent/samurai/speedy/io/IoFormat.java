package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    YAML("application/yaml;charset=UTF-8", new YAMLMapper()),
    XML("application/xml;charset=UTF-8", new XmlMapper());

    public final String media;

    private final ObjectMapper mapper;

    IoFormat(String media, ObjectMapper mapper) {
        this.media = media;
        this.mapper = mapper;
    }

    public String write(JsonNode body) {
        if (this == XML) {
            return writeXml(body);
        }
        try {
            return mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize " + this + " body", e);
        }
    }

    public JsonNode tree(MvcResult result) {
        try {
            JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
            if (this == XML) {
                node = unwrapXmlResponse(node);
            }
            return node;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse " + this + " response", e);
        }
    }

    public void assertNegotiated(MvcResult result) {
        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType, "response had no Content-Type");
        assertTrue(contentType.contains(media),
                "expected " + media + " content type but was " + contentType);
    }

    private static String writeXml(JsonNode body) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        if (body.isArray()) {
            sb.append("<root>");
            for (JsonNode item : body) {
                sb.append("<entity>");
                appendXmlFields(sb, item);
                sb.append("</entity>");
            }
            sb.append("</root>");
        } else if (body.isObject()) {
            sb.append("<root>");
            appendXmlFields(sb, body);
            sb.append("</root>");
        }
        return sb.toString();
    }

    private static void appendXmlFields(StringBuilder sb, JsonNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String rawKey = entry.getKey();
                String key = xmlElementName(rawKey);
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    sb.append('<').append(key).append('>');
                    sb.append(escapeXml(value.asText()));
                    sb.append("</").append(key).append('>');
                } else if (value.isNumber()) {
                    sb.append('<').append(key).append('>');
                    sb.append(value.asText());
                    sb.append("</").append(key).append('>');
                } else if (value.isBoolean()) {
                    sb.append('<').append(key).append('>');
                    sb.append(value.asText());
                    sb.append("</").append(key).append('>');
                } else if (value.isNull()) {
                    sb.append('<').append(key).append("/>");
                } else if (value.isObject()) {
                    sb.append('<').append(key).append('>');
                    appendXmlFields(sb, value);
                    sb.append("</").append(key).append('>');
                } else if (value.isArray()) {
                    sb.append('<').append(key).append('>');
                    for (JsonNode item : value) {
                        sb.append("<entity>");
                        appendXmlFields(sb, item);
                        sb.append("</entity>");
                    }
                    sb.append("</").append(key).append('>');
                }
            });
        }
    }

    private static String xmlElementName(String key) {
        return key.startsWith("$") ? key.substring(1) : key;
    }

    private static String escapeXml(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static JsonNode unwrapXmlResponse(JsonNode node) {
        return walk(new ObjectMapper().createObjectNode(), node);
    }

    private static JsonNode walk(ObjectNode target, JsonNode source) {
        if (!source.isObject()) return source;
        ((ObjectNode) source).fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isObject() && isEntityWrapper((ObjectNode) value)) {
                target.set(key, wrapInArray(unwrapChildren((ObjectNode) value)));
            } else if (value.isArray()) {
                ArrayNode arr = target.arrayNode();
                for (JsonNode item : value) {
                    if (item.isObject()) {
                        arr.add(unwrapChildren((ObjectNode) item));
                    } else {
                        arr.add(item);
                    }
                }
                target.set(key, arr);
            } else {
                target.set(key, value);
            }
        });
        return target;
    }

    private static boolean isEntityWrapper(ObjectNode node) {
        if (node.size() != 1) return false;
        JsonNode child = node.get(node.fieldNames().next());
        return child.isArray() || (child.isObject() && child.size() > 0);
    }

    private static JsonNode unwrapChildren(ObjectNode wrapper) {
        JsonNode child = wrapper.get(wrapper.fieldNames().next());
        if (child.isArray()) {
            return child;
        }
        return walk(JsonNodeFactory.instance.objectNode(), child);
    }

    private static ArrayNode wrapInArray(JsonNode value) {
        if (value.isArray()) {
            return (ArrayNode) value;
        }
        ArrayNode arr = JsonNodeFactory.instance.arrayNode();
        arr.add(value);
        return arr;
    }
}
