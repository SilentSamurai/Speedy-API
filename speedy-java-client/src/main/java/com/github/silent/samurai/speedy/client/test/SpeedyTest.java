package com.github.silent.samurai.speedy.client.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.internal.PathBuilder;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

/**
 * Test facade for MockMvc-based integration testing. Same builder API as
 * {@link com.github.silent.samurai.speedy.client.Speedy} but returns
 * {@link SpeedyTestResult} with assertion methods.
 *
 * <p>Errors (4xx/5xx) are NOT thrown as exceptions — instead, the result
 * carries the status code for explicit assertion via {@link SpeedyTestResult#expectBadRequest()}
 * and similar methods.
 *
 * <pre>{@code
 * SpeedyTest speedy = SpeedyTest.mockMvc(mockMvc);
 *
 * speedy.create("User")
 *     .field("name", "Test")
 *     .execute()
 *     .expectOk()
 *     .expectJsonPath("$.payload[0].name", equalTo("Test"));
 *
 * User user = speedy.get("User").key("id", id).execute()
 *     .expectOk()
 *     .first(User.class);
 * }</pre>
 */
public class SpeedyTest {

    private final MockMvcTransport transport;
    private final ObjectMapper mapper;
    private final PathBuilder paths;
    private final ResponseParser parser;

    private SpeedyTest(MockMvcTransport transport, ObjectMapper mapper) {
        this.transport = transport;
        this.mapper = mapper;
        this.paths = new PathBuilder("http://localhost", "/speedy/v1/");
        this.parser = new ResponseParser(mapper);
    }

    /**
     * Creates a test facade using the provided {@link MockMvc} instance.
     */
    public static SpeedyTest mockMvc(MockMvc mockMvc) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new SpeedyTest(new MockMvcTransport(mockMvc), mapper);
    }

    public TestCreateBuilder create(String entity) {
        return new TestCreateBuilder(entity);
    }

    public SpeedyTestResult createMany(String entity, List<ObjectNode> entities) {
        String url = paths.createPath(entity);
        ArrayNode array = mapper.createArrayNode();
        entities.forEach(array::add);
        try {
            return execute(url, "POST", mapper.writeValueAsString(array));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize createMany body", e);
        }
    }

    public TestGetBuilder get(String entity) {
        return new TestGetBuilder(entity);
    }

    public TestUpdateBuilder update(String entity) {
        return new TestUpdateBuilder(entity);
    }

    public TestDeleteBuilder delete(String entity) {
        return new TestDeleteBuilder(entity);
    }

    public SpeedyTestResult deleteMany(String entity, List<ObjectNode> pks) {
        String url = paths.deletePath(entity);
        ArrayNode array = mapper.createArrayNode();
        pks.forEach(array::add);
        try {
            return execute(url, "DELETE", mapper.writeValueAsString(array));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize deleteMany body", e);
        }
    }

    public TestQueryBuilder query(String entity) {
        return new TestQueryBuilder(entity);
    }

    SpeedyTestResult execute(String url, String method, String body) {
        try {
            SpeedyRawResponse response = transport.send(
                    new com.github.silent.samurai.speedy.client.transport.SpeedyRequest(
                            method, url, java.util.Collections.emptyMap(), body));
            ResultActions resultActions = transport.getLastResultActions();
            SpeedyResult speedyResult = response.is2xx()
                    ? parser.parseEntityResponse(response)
                    : new SpeedyResult(mapper.createArrayNode(), 0, 0, mapper);
            return new SpeedyTestResult(resultActions, speedyResult.raw(), mapper);
        } catch (Exception e) {
            throw new RuntimeException("Test request failed", e);
        }
    }

    /**
     * Test variant of {@link CreateBuilder}.
     */
    public class TestCreateBuilder {
        private final String entity;
        private final ObjectNode body;

        TestCreateBuilder(String entity) {
            this.entity = entity;
            this.body = mapper.createObjectNode();
        }

        public TestCreateBuilder field(String name, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(body, name, value);
            return this;
        }

        public SpeedyTestResult execute() {
            String url = paths.createPath(entity);
            ArrayNode array = mapper.createArrayNode();
            array.add(body);
            String jsonBody;
            try {
                jsonBody = mapper.writeValueAsString(array);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize", e);
            }
            return SpeedyTest.this.execute(url, "POST", jsonBody);
        }
    }

    public class TestGetBuilder {
        private final String entity;
        private final ObjectNode pkNode;

        TestGetBuilder(String entity) {
            this.entity = entity;
            this.pkNode = mapper.createObjectNode();
        }

        public TestGetBuilder key(String field, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(pkNode, field, value);
            return this;
        }

        public SpeedyTestResult execute() {
            String url = paths.entityPath(entity);
            String qs = paths.formatPk(pkNode);
            if (qs != null && !qs.isEmpty()) {
                url = url + "?" + qs;
            }
            return SpeedyTest.this.execute(url, "GET", null);
        }
    }

    public class TestUpdateBuilder {
        private final String entity;
        private final ObjectNode body;
        private final ObjectNode pkNode;

        TestUpdateBuilder(String entity) {
            this.entity = entity;
            this.body = mapper.createObjectNode();
            this.pkNode = mapper.createObjectNode();
        }

        public TestUpdateBuilder key(String field, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(pkNode, field, value);
            return this;
        }

        public TestUpdateBuilder field(String name, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(body, name, value);
            return this;
        }

        public SpeedyTestResult execute() {
            String url = paths.updatePath(entity);
            body.setAll(pkNode);
            String jsonBody;
            try {
                jsonBody = mapper.writeValueAsString(body);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize", e);
            }
            return SpeedyTest.this.execute(url, "PATCH", jsonBody);
        }
    }

    public class TestDeleteBuilder {
        private final String entity;
        private final ObjectNode pkNode;

        TestDeleteBuilder(String entity) {
            this.entity = entity;
            this.pkNode = mapper.createObjectNode();
        }

        public TestDeleteBuilder key(String field, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(pkNode, field, value);
            return this;
        }

        public SpeedyTestResult execute() {
            String url = paths.deletePath(entity);
            ArrayNode array = mapper.createArrayNode();
            array.add(pkNode);
            String jsonBody;
            try {
                jsonBody = mapper.writeValueAsString(array);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize", e);
            }
            return SpeedyTest.this.execute(url, "DELETE", jsonBody);
        }
    }

    public class TestQueryBuilder {
        private final String entity;
        private final ObjectNode body;

        TestQueryBuilder(String entity) {
            this.entity = entity;
            this.body = mapper.createObjectNode();
            this.body.put("$from", entity);
        }

        public TestQueryBuilder where(JsonNode... conditions) {
            if (conditions == null) return this;
            for (JsonNode condition : conditions) {
                if (condition.has("$and") || condition.has("$or")) {
                    body.remove("$where");
                    body.setAll((ObjectNode) condition);
                    return this;
                }
                if (!body.has("$where")) body.set("$where", mapper.createObjectNode());
                ObjectNode whereNode = (ObjectNode) body.get("$where");
                condition.fields().forEachRemaining(e -> whereNode.set(e.getKey(), e.getValue()));
            }
            return this;
        }

        public TestQueryBuilder orderByAsc(String field) {
            if (!body.has("$orderBy")) body.set("$orderBy", mapper.createObjectNode());
            ((ObjectNode) body.get("$orderBy")).put(field, "ASC");
            return this;
        }

        public TestQueryBuilder orderByDesc(String field) {
            if (!body.has("$orderBy")) body.set("$orderBy", mapper.createObjectNode());
            ((ObjectNode) body.get("$orderBy")).put(field, "DESC");
            return this;
        }

        public TestQueryBuilder pageNo(int n) {
            ObjectNode p = body.has("$page") ? (ObjectNode) body.get("$page") : mapper.createObjectNode();
            p.put("$index", n);
            body.set("$page", p);
            return this;
        }

        public TestQueryBuilder pageSize(int n) {
            ObjectNode p = body.has("$page") ? (ObjectNode) body.get("$page") : mapper.createObjectNode();
            p.put("$size", n);
            body.set("$page", p);
            return this;
        }

        public TestQueryBuilder select(String... fields) {
            ArrayNode a = body.has("$select") ? (ArrayNode) body.get("$select") : mapper.createArrayNode();
            for (String f : fields) a.add(f);
            body.set("$select", a);
            return this;
        }

        public TestQueryBuilder expand(String... relations) {
            ArrayNode a = body.has("$expand") ? (ArrayNode) body.get("$expand") : mapper.createArrayNode();
            for (String r : relations) a.add(r);
            body.set("$expand", a);
            return this;
        }

        public SpeedyTestResult execute() {
            String url = paths.queryPath(entity);
            String jsonBody;
            try {
                jsonBody = mapper.writeValueAsString(body);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize", e);
            }
            return SpeedyTest.this.execute(url, "POST", jsonBody);
        }
    }
}
