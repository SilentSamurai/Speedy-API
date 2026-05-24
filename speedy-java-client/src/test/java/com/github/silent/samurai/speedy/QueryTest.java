package com.github.silent.samurai.speedy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import org.junit.jupiter.api.Test;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
import static org.junit.jupiter.api.Assertions.*;

class QueryTest {

    @Test
    void shouldBuildBasicQuery() throws JsonProcessingException {
        JsonNode build = from("users")
                .where(condition("active", eq(true)))
                .build();

        assertEquals("users", build.get("$from").asText());
        assertTrue(build.get("$where").get("active").get("$eq").asBoolean());
    }

    @Test
    void shouldBuildQueryWithMultipleSimpleConditions() throws JsonProcessingException {
        JsonNode build = from("users")
                .where(
                        condition("id", eq("1")),
                        condition("name", eq("John"))
                )
                .build();

        assertEquals("1", build.get("$where").get("id").get("$eq").asText());
        assertEquals("John", build.get("$where").get("name").get("$eq").asText());
    }

    @Test
    void shouldReplaceWhereOnSecondCall() throws JsonProcessingException {
        JsonNode build = from("users")
                .where(condition("id", eq("1")))
                .where(condition("name", eq("John")))
                .build();

        assertFalse(build.get("$where").has("id"), "First condition should be replaced");
        assertEquals("John", build.get("$where").get("name").get("$eq").asText());
    }

    @Test
    void shouldBuildAndCondition() throws JsonProcessingException {
        JsonNode build = from("users")
                .where(
                        and(
                                condition("active", eq(true)),
                                condition("age", gte(18))
                        )
                )
                .build();

        assertTrue(build.get("$where").has("$and"));
        assertEquals(2, build.get("$where").get("$and").size());
    }

    @Test
    void shouldBuildOrCondition() throws JsonProcessingException {
        JsonNode build = from("users")
                .where(
                        or(
                                condition("role", eq("admin")),
                                condition("role", eq("moderator"))
                        )
                )
                .build();

        assertTrue(build.get("$where").has("$or"));
        assertEquals(2, build.get("$where").get("$or").size());
    }

    @Test
    void shouldBuildWithAllComparisonOperators() throws JsonProcessingException {
        JsonNode eqNode = eq("test");
        assertEquals("test", eqNode.get("$eq").asText());

        JsonNode neNode = ne("test");
        assertEquals("test", neNode.get("$ne").asText());

        JsonNode gtNode = gt(10);
        assertEquals(10, gtNode.get("$gt").asInt());

        JsonNode ltNode = lt(100);
        assertEquals(100, ltNode.get("$lt").asInt());

        JsonNode gteNode = gte(18);
        assertEquals(18, gteNode.get("$gte").asInt());

        JsonNode lteNode = lte(65);
        assertEquals(65, lteNode.get("$lte").asInt());

        JsonNode inNode = in("a", "b", "c");
        assertEquals(3, inNode.get("$in").size());

        JsonNode ninNode = nin("x", "y");
        assertEquals(2, ninNode.get("$nin").size());

        JsonNode matchesNode = matches("john.*");
        assertEquals("john.*", matchesNode.get("$matches").asText());

        JsonNode containsNode = contains("john");
        assertEquals("john", containsNode.get("$contains").asText());
    }

    @Test
    void shouldBuildWithPagination() {
        JsonNode build = from("users")
                .pageNo(2)
                .pageSize(50)
                .build();

        assertEquals(2, build.get("$page").get("$index").asInt());
        assertEquals(50, build.get("$page").get("$size").asInt());
    }

    @Test
    void shouldRejectNegativePageNo() {
        assertThrows(IllegalArgumentException.class, () -> from("users").pageNo(-1));
    }

    @Test
    void shouldRejectZeroPageSize() {
        assertThrows(IllegalArgumentException.class, () -> from("users").pageSize(0));
    }

    @Test
    void shouldBuildWithOrderBy() {
        JsonNode build = from("users")
                .orderByAsc("name")
                .orderByDesc("createdAt")
                .build();

        assertEquals("ASC", build.get("$orderBy").get("name").asText());
        assertEquals("DESC", build.get("$orderBy").get("createdAt").asText());
    }

    @Test
    void shouldBuildWithExpand() {
        JsonNode build = from("users")
                .expand("profile")
                .expand("permissions")
                .build();

        assertEquals(2, build.get("$expand").size());
        assertEquals("profile", build.get("$expand").get(0).asText());
        assertEquals("permissions", build.get("$expand").get(1).asText());
    }

    @Test
    void shouldBuildWithSelect() {
        JsonNode build = from("users")
                .select("id", "name", "email")
                .build();

        assertEquals(3, build.get("$select").size());
        assertTrue(build.get("$select").toString().contains("id"));
        assertTrue(build.get("$select").toString().contains("name"));
        assertTrue(build.get("$select").toString().contains("email"));
    }

    @Test
    void shouldBuildComplexNestedQuery() throws JsonProcessingException {
        JsonNode build = from("users")
                .where(
                        and(
                                or(
                                        condition("role", eq("admin")),
                                        condition("role", eq("moderator"))
                                ),
                                condition("active", eq(true))
                        )
                )
                .select("id", "name")
                .expand("profile")
                .orderByDesc("createdAt")
                .pageNo(0)
                .pageSize(25)
                .build();

        assertNotNull(build.get("$where").get("$and"));
        assertEquals(2, build.get("$where").get("$and").size());
        assertEquals(2, build.get("$select").size());
        assertEquals(1, build.get("$expand").size());
        assertEquals("DESC", build.get("$orderBy").get("createdAt").asText());
    }

    @Test
    void shouldPrettyPrintWithoutThrowing() throws JsonProcessingException {
        assertDoesNotThrow(() -> from("users")
                .where(condition("active", eq(true)))
                .prettyPrint()
                .build());
    }

    @Test
    void shouldThrowOnNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> SpeedyQuery.from(null));
    }

    @Test
    void shouldBuildQueryWithNullWhere() {
        JsonNode build = from("users").build();
        assertFalse(build.has("$where"));
    }
}
