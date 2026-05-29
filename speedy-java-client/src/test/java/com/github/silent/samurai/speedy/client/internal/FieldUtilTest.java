package com.github.silent.samurai.speedy.client.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldUtilTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void simpleFieldShouldBeSetDirectly() {
        ObjectNode root = mapper.createObjectNode();
        FieldUtil.setField(root, "name", "Alice");
        assertEquals("Alice", root.get("name").asText());
    }

    @Test
    void dotNotationShouldCreateNestedObjects() {
        ObjectNode root = mapper.createObjectNode();
        FieldUtil.setField(root, "address.city", "Seattle");
        assertTrue(root.has("address"));
        assertTrue(root.get("address").isObject());
        assertEquals("Seattle", root.get("address").get("city").asText());
    }

    @Test
    void deepNestingShouldCreateIntermediateNodes() {
        ObjectNode root = mapper.createObjectNode();
        FieldUtil.setField(root, "a.b.c.d", "deep");
        assertEquals("deep", root.get("a").get("b").get("c").get("d").asText());
    }

    @Test
    void overwriteExistingShouldPreservePeerFields() {
        ObjectNode root = mapper.createObjectNode();
        FieldUtil.setField(root, "address.city", "Seattle");
        FieldUtil.setField(root, "address.zip", "98101");
        assertEquals("Seattle", root.get("address").get("city").asText());
        assertEquals("98101", root.get("address").get("zip").asText());
    }

    @Test
    void intValueShouldBeSetCorrectly() {
        ObjectNode root = mapper.createObjectNode();
        FieldUtil.setField(root, "age", 25);
        assertEquals(25, root.get("age").asInt());
    }

    @Test
    void booleanValueShouldBeSetCorrectly() {
        ObjectNode root = mapper.createObjectNode();
        FieldUtil.setField(root, "active", true);
        assertTrue(root.get("active").asBoolean());
    }

    @Test
    void nullValueShouldSetNullNode() {
        ObjectNode root = mapper.createObjectNode();
        FieldUtil.setField(root, "optional", null);
        assertTrue(root.get("optional").isNull());
    }
}
