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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    public TestBulkCreateBuilder createMany(String entity) {
        return new TestBulkCreateBuilder(entity);
    }

    public SpeedyTestResult createMany(String entity, List<ObjectNode> entities) {
        return new TestBulkCreateBuilder(entity).items(entities).execute();
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

    public TestBulkDeleteBuilder deleteMany(String entity) {
        return new TestBulkDeleteBuilder(entity);
    }

    public SpeedyTestResult deleteMany(String entity, List<ObjectNode> pks) {
        return new TestBulkDeleteBuilder(entity).items(pks).execute();
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
        private final List<String> selectFields;
        private final List<String> expandRelations;
        private Integer pageSize;
        private Integer pageNo;

        TestGetBuilder(String entity) {
            this.entity = entity;
            this.pkNode = mapper.createObjectNode();
            this.selectFields = new ArrayList<>();
            this.expandRelations = new ArrayList<>();
        }

        public TestGetBuilder key(String field, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(pkNode, field, value);
            return this;
        }

        public TestGetBuilder select(String... fields) {
            for (String field : fields) {
                selectFields.add(field);
            }
            return this;
        }

        public TestGetBuilder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public TestGetBuilder pageNo(int pageNo) {
            this.pageNo = pageNo;
            return this;
        }

        public TestGetBuilder expand(String... relations) {
            for (String relation : relations) {
                expandRelations.add(relation);
            }
            return this;
        }

        public SpeedyTestResult execute() {
            String url = paths.entityPath(entity);
            String qs = buildQueryString();
            if (!qs.isEmpty()) {
                url = url + "?" + qs;
            }
            return SpeedyTest.this.execute(url, "GET", null);
        }

        private String buildQueryString() {
            StringBuilder sb = new StringBuilder();

            Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = pkNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
                if (sb.length() > 0) sb.append("&");
                sb.append(entry.getKey()).append("=").append(entry.getValue().asText());
            }

            if (!selectFields.isEmpty()) {
                if (sb.length() > 0) sb.append("&");
                sb.append("$select=").append(String.join(",", selectFields));
            }

            if (pageSize != null) {
                if (sb.length() > 0) sb.append("&");
                sb.append("$pageSize=").append(pageSize);
            }

            if (pageNo != null) {
                if (sb.length() > 0) sb.append("&");
                sb.append("$pageNo=").append(pageNo);
            }

            if (!expandRelations.isEmpty()) {
                if (sb.length() > 0) sb.append("&");
                sb.append("$expand=").append(String.join(",", expandRelations));
            }

            return sb.toString();
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

    public class TestBulkCreateBuilder {
        private final String entity;
        private List<ObjectNode> items;
        private String transactionMode;

        TestBulkCreateBuilder(String entity) {
            this.entity = entity;
        }

        public TestBulkCreateBuilder items(List<ObjectNode> items) {
            this.items = items;
            return this;
        }

        public TestBulkCreateBuilder transaction(String mode) {
            this.transactionMode = mode;
            return this;
        }

        public SpeedyTestResult execute() {
            String url = paths.createPath(entity);
            if (transactionMode != null && !transactionMode.isEmpty()) {
                url += "?$transaction=" + transactionMode;
            }
            ArrayNode array = mapper.createArrayNode();
            items.forEach(array::add);
            try {
                return SpeedyTest.this.execute(url, "POST", mapper.writeValueAsString(array));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize createMany body", e);
            }
        }
    }

    public class TestBulkDeleteBuilder {
        private final String entity;
        private List<ObjectNode> items;
        private String transactionMode;

        TestBulkDeleteBuilder(String entity) {
            this.entity = entity;
        }

        public TestBulkDeleteBuilder items(List<ObjectNode> items) {
            this.items = items;
            return this;
        }

        public TestBulkDeleteBuilder transaction(String mode) {
            this.transactionMode = mode;
            return this;
        }

        public SpeedyTestResult execute() {
            String url = paths.deletePath(entity);
            if (transactionMode != null && !transactionMode.isEmpty()) {
                url += "?$transaction=" + transactionMode;
            }
            ArrayNode array = mapper.createArrayNode();
            items.forEach(array::add);
            try {
                return SpeedyTest.this.execute(url, "DELETE", mapper.writeValueAsString(array));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize deleteMany body", e);
            }
        }
    }
}
