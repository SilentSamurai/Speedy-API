package com.github.silent.samurai.speedy.client.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles dot-notation field paths for create/update builders.
 *
 * <p>Example: {@code setField(rootNode, "address.city", "Seattle")}
 * produces {@code {"address": {"city": "Seattle"}}}.
 */
public final class FieldUtil {

    private FieldUtil() {
    }

    /**
     * Sets a field value at the given dot-notation path on an ObjectNode.
     * Creates intermediate nested ObjectNode(s) as needed.
     *
     * @param root  the root ObjectNode to set the field on
     * @param path  dot-notation field path (e.g., "address.city")
     * @param value the value to set (may be primitives, JsonNode, or other objects)
     */
    public static void setField(ObjectNode root, String path, Object value) {
        String[] parts = path.split("\\.");
        ObjectNode current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            JsonNode existing = current.get(part);
            if (existing == null || !existing.isObject()) {
                current = current.putObject(part);
            } else {
                current = (ObjectNode) existing;
            }
        }
        String leaf = parts[parts.length - 1];
        if (value instanceof JsonNode) {
            current.set(leaf, (JsonNode) value);
        } else if (value instanceof String) {
            current.put(leaf, (String) value);
        } else if (value instanceof Integer) {
            current.put(leaf, (Integer) value);
        } else if (value instanceof Long) {
            current.put(leaf, (Long) value);
        } else if (value instanceof Double) {
            current.put(leaf, (Double) value);
        } else if (value instanceof Float) {
            current.put(leaf, (Float) value);
        } else if (value instanceof Boolean) {
            current.put(leaf, (Boolean) value);
        } else if (value instanceof Byte) {
            current.put(leaf, (Byte) value);
        } else if (value instanceof Short) {
            current.put(leaf, (Short) value);
        } else if (value == null) {
            current.putNull(leaf);
        } else {
            current.putPOJO(leaf, value);
        }
    }
}
