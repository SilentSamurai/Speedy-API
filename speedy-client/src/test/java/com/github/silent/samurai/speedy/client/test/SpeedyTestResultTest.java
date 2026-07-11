package com.github.silent.samurai.speedy.client.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeedyTestResultTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void listShouldDeserializeTypedEntities() {
        ArrayNode payload = mapper.createArrayNode();
        payload.addObject().put("name", "Alice");
        payload.addObject().put("name", "Bob");

        SpeedyTestResult result = new SpeedyTestResult(null, payload, mapper);
        var list = result.list(Person.class);
        assertEquals(2, list.size());
        assertEquals("Alice", list.get(0).name);
    }

    @Test
    void firstShouldReturnFirstEntity() {
        ArrayNode payload = mapper.createArrayNode();
        payload.addObject().put("name", "Bob");

        SpeedyTestResult result = new SpeedyTestResult(null, payload, mapper);
        Person p = result.first(Person.class);
        assertNotNull(p);
        assertEquals("Bob", p.name);
    }

    @Test
    void firstShouldReturnNullForEmptyPayload() {
        SpeedyTestResult result = new SpeedyTestResult(null, mapper.createArrayNode(), mapper);
        assertNull(result.first(Person.class));
    }

    public static class Person {
        public String name;
    }
}
