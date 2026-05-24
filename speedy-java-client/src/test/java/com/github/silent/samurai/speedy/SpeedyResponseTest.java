package com.github.silent.samurai.speedy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpeedyResponseTest {

    private static final ObjectMapper MAPPER = CommonUtil.json();

    static class TestEntity {
        private String name;
        private int age;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    @Test
    void asListShouldDeserializeArrayPayload() {
        ArrayNode array = MAPPER.createArrayNode();
        ObjectNode entity1 = MAPPER.createObjectNode().put("name", "Alice").put("age", 30);
        ObjectNode entity2 = MAPPER.createObjectNode().put("name", "Bob").put("age", 25);
        array.add(entity1);
        array.add(entity2);

        SpeedyResponse response = new SpeedyResponse();
        response.setPayload(array);

        List<TestEntity> entities = response.asList(TestEntity.class);

        assertEquals(2, entities.size());
        assertEquals("Alice", entities.get(0).getName());
        assertEquals(30, entities.get(0).getAge());
        assertEquals("Bob", entities.get(1).getName());
    }

    @Test
    void asListShouldHandleNullPayload() {
        SpeedyResponse response = new SpeedyResponse();
        response.setPayload(null);

        List<TestEntity> entities = response.asList(TestEntity.class);
        assertTrue(entities.isEmpty());
    }

    @Test
    void asSingleShouldDeserializeFirstElement() {
        ObjectNode entity = MAPPER.createObjectNode().put("name", "Alice").put("age", 30);

        SpeedyResponse response = new SpeedyResponse();
        response.setPayload(entity);

        TestEntity result = response.asSingle(TestEntity.class);

        assertEquals("Alice", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    void asSingleShouldReturnFirstFromArray() {
        ArrayNode array = MAPPER.createArrayNode();
        array.add(MAPPER.createObjectNode().put("name", "Alice").put("age", 30));
        array.add(MAPPER.createObjectNode().put("name", "Bob").put("age", 25));

        SpeedyResponse response = new SpeedyResponse();
        response.setPayload(array);

        TestEntity result = response.asSingle(TestEntity.class);
        assertEquals("Alice", result.getName());
    }

    @Test
    void asSingleShouldReturnNullForNullPayload() {
        SpeedyResponse response = new SpeedyResponse();
        assertNull(response.asSingle(TestEntity.class));
    }

    @Test
    void asCountShouldReturnPayloadAsLong() {
        SpeedyResponse response = new SpeedyResponse();
        response.setPayload(MAPPER.getNodeFactory().numberNode(42));

        assertEquals(42L, response.asCount());
    }

    @Test
    void asCountShouldReturnZeroForNullPayload() {
        SpeedyResponse response = new SpeedyResponse();
        assertEquals(0L, response.asCount());
    }

    @Test
    void paginationFieldsShouldBeAccessible() {
        SpeedyResponse response = new SpeedyResponse();
        response.setPageIndex(0);
        response.setPageSize(20);
        response.setTotalPageCount(5);

        assertEquals(0, response.getPageIndex());
        assertEquals(20, response.getPageSize());
        assertEquals(5, response.getTotalPageCount());
    }
}
