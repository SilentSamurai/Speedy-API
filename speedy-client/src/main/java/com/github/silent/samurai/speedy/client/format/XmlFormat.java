package com.github.silent.samurai.speedy.client.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.silent.samurai.speedy.client.exception.SpeedyDeserializationException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * XML wire format, matching the server's {@code speedy-xml-io} provider
 * ({@code application/xml}). Built on JDK StAX — no extra dependencies.
 *
 * <p><b>Request shape</b> (what the server's {@code XmlStructureReader} expects):
 * {@code <root>} document element; array bodies emit one {@code <entity>} per
 * item; nested object arrays also use {@code <entity>}, scalar arrays use
 * {@code <item>}; a leading {@code $} is stripped from element names
 * ({@code $where} → {@code <where>}) since {@code $} is illegal at the start of
 * an XML name — the server accepts both spellings.
 *
 * <p><b>Response shape</b>: XML has no native arrays, so the server wraps
 * repeated entities in named elements ({@code <payload><Category>…</Category>
 * <Category>…</Category></payload>}). {@link #read} unwraps those back into
 * JSON arrays so the standard envelope contract holds.
 */
public final class XmlFormat implements SpeedyFormat {

    public static final String CONTENT_TYPE = "application/xml;charset=UTF-8";

    private static final String ROOT = "root";
    private static final String ENTITY_ITEM = "entity";
    private static final String SCALAR_ITEM = "item";

    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newFactory();

    static {
        INPUT_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        INPUT_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    @Override
    public String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public String write(JsonNode tree) {
        StringWriter out = new StringWriter();
        try {
            XMLStreamWriter w = OUTPUT_FACTORY.createXMLStreamWriter(out);
            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement(ROOT);
            if (tree != null && tree.isArray()) {
                for (JsonNode item : tree) {
                    writeValue(w, ENTITY_ITEM, item);
                }
            } else if (tree != null && tree.isObject()) {
                writeFields(w, tree);
            }
            w.writeEndElement();
            w.writeEndDocument();
            w.close();
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
        return out.toString();
    }

    private void writeFields(XMLStreamWriter w, JsonNode node) throws XMLStreamException {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            writeValue(w, elementName(entry.getKey()), entry.getValue());
        }
    }

    private void writeValue(XMLStreamWriter w, String name, JsonNode value) throws XMLStreamException {
        if (value == null || value.isNull()) {
            w.writeEmptyElement(name);
            return;
        }
        if (value.isObject()) {
            w.writeStartElement(name);
            writeFields(w, value);
            w.writeEndElement();
            return;
        }
        if (value.isArray()) {
            w.writeStartElement(name);
            for (JsonNode item : value) {
                writeValue(w, item.isObject() ? ENTITY_ITEM : SCALAR_ITEM, item);
            }
            w.writeEndElement();
            return;
        }
        w.writeStartElement(name);
        w.writeCharacters(value.asText());
        w.writeEndElement();
    }

    private static String elementName(String key) {
        return key.startsWith("$") ? key.substring(1) : key;
    }

    @Override
    public JsonNode read(String body) {
        try {
            XMLStreamReader r = INPUT_FACTORY.createXMLStreamReader(new StringReader(body));
            JsonNode root = null;
            while (r.hasNext()) {
                if (r.next() == XMLStreamReader.START_ELEMENT) {
                    root = parseElement(r);
                    break;
                }
            }
            r.close();
            if (root == null) {
                throw new SpeedyDeserializationException("Empty XML response");
            }
            return unwrapArrays(root);
        } catch (XMLStreamException e) {
            throw new SpeedyDeserializationException("Failed to parse XML response: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the element the reader is positioned on into a tree node: a leaf
     * becomes text (or null when empty), a container becomes an object with
     * repeated same-name children collected into arrays.
     */
    private JsonNode parseElement(XMLStreamReader r) throws XMLStreamException {
        Map<String, List<JsonNode>> children = new LinkedHashMap<>();
        StringBuilder text = new StringBuilder();
        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamReader.START_ELEMENT) {
                children.computeIfAbsent(r.getLocalName(), k -> new ArrayList<>()).add(parseElement(r));
            } else if (event == XMLStreamReader.CHARACTERS || event == XMLStreamReader.CDATA) {
                text.append(r.getText());
            } else if (event == XMLStreamReader.END_ELEMENT) {
                break;
            }
        }
        if (children.isEmpty()) {
            String raw = text.toString();
            return raw.isEmpty() ? NullNode.getInstance() : TextNode.valueOf(raw);
        }
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, List<JsonNode>> entry : children.entrySet()) {
            List<JsonNode> nodes = entry.getValue();
            if (nodes.size() == 1) {
                obj.set(entry.getKey(), nodes.get(0));
            } else {
                ArrayNode arr = JsonNodeFactory.instance.arrayNode();
                nodes.forEach(arr::add);
                obj.set(entry.getKey(), arr);
            }
        }
        return obj;
    }

    /**
     * Restores JSON array semantics lost in XML: an object whose only field is
     * the entity-name wrapper ({@code {"payload":{"Category":[…]}}}) collapses
     * to the inner value coerced to an array.
     */
    private JsonNode unwrapArrays(JsonNode source) {
        if (!source.isObject()) {
            return source;
        }
        ObjectNode target = JsonNodeFactory.instance.objectNode();
        Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            if (value.isObject() && isEntityWrapper((ObjectNode) value)) {
                target.set(entry.getKey(), wrapInArray(unwrapChildren((ObjectNode) value)));
            } else if (value.isArray()) {
                ArrayNode arr = JsonNodeFactory.instance.arrayNode();
                for (JsonNode item : value) {
                    arr.add(unwrapArrays(item));
                }
                target.set(entry.getKey(), arr);
            } else if (value.isObject()) {
                target.set(entry.getKey(), unwrapArrays(value));
            } else {
                target.set(entry.getKey(), value);
            }
        }
        return target;
    }

    private static boolean isEntityWrapper(ObjectNode node) {
        if (node.size() != 1) {
            return false;
        }
        JsonNode child = node.get(node.fieldNames().next());
        return child.isArray() || (child.isObject() && !child.isEmpty());
    }

    private JsonNode unwrapChildren(ObjectNode wrapper) {
        JsonNode child = wrapper.get(wrapper.fieldNames().next());
        if (child.isArray()) {
            ArrayNode arr = JsonNodeFactory.instance.arrayNode();
            for (JsonNode item : child) {
                arr.add(unwrapArrays(item));
            }
            return arr;
        }
        return unwrapArrays(child);
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
