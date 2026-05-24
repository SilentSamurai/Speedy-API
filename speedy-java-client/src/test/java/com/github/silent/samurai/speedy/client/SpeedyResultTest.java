package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SpeedyResultTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void listShouldDeserializeTypedEntities() {
        ArrayNode payload = mapper.createArrayNode();
        payload.addObject().put("name", "Alice").put("age", 30);
        payload.addObject().put("name", "Bob").put("age", 25);

        SpeedyResult result = new SpeedyResult(payload, 0, 10, mapper);
        List<Person> people = result.list(Person.class);

        assertEquals(2, people.size());
        assertEquals("Alice", people.get(0).getName());
        assertEquals(30, people.get(0).getAge());
        assertEquals("Bob", people.get(1).getName());
    }

    @Test
    void firstShouldReturnFirstEntity() {
        ArrayNode payload = mapper.createArrayNode();
        payload.addObject().put("name", "Alice");
        payload.addObject().put("name", "Bob");

        SpeedyResult result = new SpeedyResult(payload, 0, 10, mapper);
        Person person = result.first(Person.class);

        assertNotNull(person);
        assertEquals("Alice", person.getName());
    }

    @Test
    void firstShouldReturnNullForEmptyPayload() {
        ArrayNode payload = mapper.createArrayNode();
        SpeedyResult result = new SpeedyResult(payload, 0, 0, mapper);
        assertNull(result.first(Person.class));
    }

    @Test
    void firstOptionalShouldReturnEmptyForEmptyPayload() {
        ArrayNode payload = mapper.createArrayNode();
        SpeedyResult result = new SpeedyResult(payload, 0, 0, mapper);
        assertEquals(Optional.empty(), result.firstOptional(Person.class));
    }

    @Test
    void isEmptyShouldReturnTrueForEmptyPayload() {
        SpeedyResult result = new SpeedyResult(mapper.createArrayNode(), 0, 0, mapper);
        assertTrue(result.isEmpty());
    }

    @Test
    void sizeShouldReturnCorrectCount() {
        ArrayNode payload = mapper.createArrayNode();
        payload.addObject().put("x", 1);
        payload.addObject().put("x", 2);
        payload.addObject().put("x", 3);

        SpeedyResult result = new SpeedyResult(payload, 1, 20, mapper);
        assertEquals(3, result.size());
        assertFalse(result.isEmpty());
    }

    @Test
    void pageMetadataShouldBePreserved() {
        SpeedyResult result = new SpeedyResult(mapper.createArrayNode(), 2, 50, mapper);
        assertEquals(2, result.pageIndex());
        assertEquals(50, result.pageSize());
    }

    @Test
    void firstRawShouldReturnFirstJsonNode() {
        ArrayNode payload = mapper.createArrayNode();
        payload.addObject().put("id", 123);

        SpeedyResult result = new SpeedyResult(payload, 0, 10, mapper);
        assertNotNull(result.firstRaw());
        assertEquals(123, result.firstRaw().get("id").asInt());
    }

    @Test
    void nullPayloadShouldDefaultToEmptyArray() {
        SpeedyResult result = new SpeedyResult(null, 0, 0, mapper);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @SuppressWarnings("unused")
    public static class Person {
        private String name;
        private int age;

        public Person() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}
