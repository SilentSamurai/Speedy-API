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
import java.util.function.Consumer;

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

    /**
     * Sends a bare JSON object (not wrapped in an array) to {@code $create} — the
     * single-entity shorthand accepted by the server.
     */
    public SpeedyTestResult createOne(String entity, ObjectNode body) {
        try {
            return execute(paths.createPath(entity), "POST", mapper.writeValueAsString(body));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize createOne body", e);
        }
    }

    public TestGetBuilder get(String entity) {
        return new TestGetBuilder(entity);
    }

    public TestUpdateBuilder update(String entity) {
        return new TestUpdateBuilder(entity);
    }

    public TestReplaceBuilder replace(String entity) {
        return new TestReplaceBuilder(entity);
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

    public TestBulkUpdateBuilder updateMany(String entity) {
        return new TestBulkUpdateBuilder(entity);
    }

    public SpeedyTestResult updateMany(String entity, List<ObjectNode> items) {
        return new TestBulkUpdateBuilder(entity).items(items).execute();
    }

    public TestBulkReplaceBuilder replaceMany(String entity) {
        return new TestBulkReplaceBuilder(entity);
    }

    public SpeedyTestResult replaceMany(String entity, List<ObjectNode> items) {
        return new TestBulkReplaceBuilder(entity).items(items).execute();
    }

    /**
     * Sends a bare JSON object (not wrapped in an array) to {@code $delete} — the
     * single-key shorthand accepted by the server.
     */
    public SpeedyTestResult deleteOne(String entity, ObjectNode pk) {
        try {
            return execute(paths.deletePath(entity), "DELETE", mapper.writeValueAsString(pk));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize deleteOne body", e);
        }
    }

    public TestQueryBuilder query(String entity) {
        return new TestQueryBuilder(entity);
    }

    SpeedyTestResult execute(String url, String method, String body) {
        return execute(url, method, body, java.util.Collections.emptyMap());
    }

    SpeedyTestResult execute(String url, String method, String body, Map<String, String> headers) {
        try {
            com.github.silent.samurai.speedy.client.transport.SpeedyRequest request =
                    new com.github.silent.samurai.speedy.client.transport.SpeedyRequest(
                            method, url, java.util.Collections.emptyMap(), body);
            if (headers != null && !headers.isEmpty()) {
                request = request.withHeaders(headers);
            }
            SpeedyRawResponse response = transport.send(request);
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

        /**
         * Builds the JSON request body without executing.
         */
        public ObjectNode build() {
            return body;
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
        private final Map<String, String> headers = new java.util.LinkedHashMap<>();
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

        /**
         * Sets a request header (e.g. {@code If-None-Match} for a conditional GET).
         */
        public TestGetBuilder header(String name, String value) {
            headers.put(name, value);
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
            return SpeedyTest.this.execute(url, "GET", null, headers);
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
        private final Map<String, String> headers = new java.util.LinkedHashMap<>();

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

        /**
         * Sets a request header (e.g. {@code If-Match} for an optimistic-concurrency update).
         */
        public TestUpdateBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        /**
         * Builds the JSON request body (key + fields merged) without executing.
         */
        public ObjectNode build() {
            body.setAll(pkNode);
            return body;
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
            return SpeedyTest.this.execute(url, "PATCH", jsonBody, headers);
        }
    }

    /** Full-replace (HTTP PUT) counterpart of {@link TestUpdateBuilder}. */
    public class TestReplaceBuilder {
        private final String entity;
        private final ObjectNode body;
        private final ObjectNode pkNode;

        TestReplaceBuilder(String entity) {
            this.entity = entity;
            this.body = mapper.createObjectNode();
            this.pkNode = mapper.createObjectNode();
        }

        public TestReplaceBuilder key(String field, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(pkNode, field, value);
            return this;
        }

        public TestReplaceBuilder field(String name, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(body, name, value);
            return this;
        }

        /**
         * Builds the JSON request body (key + fields merged) without executing.
         */
        public ObjectNode build() {
            body.setAll(pkNode);
            return body;
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
            return SpeedyTest.this.execute(url, "PUT", jsonBody);
        }
    }

    public class TestDeleteBuilder {
        private final String entity;
        private final ObjectNode pkNode;
        private final Map<String, String> headers = new java.util.LinkedHashMap<>();

        TestDeleteBuilder(String entity) {
            this.entity = entity;
            this.pkNode = mapper.createObjectNode();
        }

        public TestDeleteBuilder key(String field, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(pkNode, field, value);
            return this;
        }

        /**
         * Sets a request header (e.g. {@code If-Match} for an optimistic-concurrency delete).
         */
        public TestDeleteBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        /**
         * Builds the JSON primary-key body without executing.
         */
        public ObjectNode build() {
            return pkNode;
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
            return SpeedyTest.this.execute(url, "DELETE", jsonBody, headers);
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

    /**
     * Builds one bulk-item body via key/field chaining, with no entity/URL context of its own —
     * used by {@code item(...)} on each {@code TestBulk*Builder} to append one array element
     * without the caller having to construct an {@link ObjectNode} by hand.
     */
    public class TestItemBuilder {
        private final ObjectNode body = mapper.createObjectNode();
        private final ObjectNode pkNode = mapper.createObjectNode();

        public TestItemBuilder key(String field, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(pkNode, field, value);
            return this;
        }

        public TestItemBuilder field(String name, Object value) {
            com.github.silent.samurai.speedy.client.internal.FieldUtil.setField(body, name, value);
            return this;
        }

        ObjectNode build() {
            body.setAll(pkNode);
            return body;
        }
    }

    public class TestBulkCreateBuilder {
        private final String entity;
        private List<ObjectNode> items = new ArrayList<>();
        private String transactionMode;

        TestBulkCreateBuilder(String entity) {
            this.entity = entity;
        }

        public TestBulkCreateBuilder items(List<ObjectNode> items) {
            this.items = items;
            return this;
        }

        /**
         * Appends one item, built via key/field chaining, to the bulk request.
         */
        public TestBulkCreateBuilder item(Consumer<TestItemBuilder> itemSpec) {
            TestItemBuilder item = new TestItemBuilder();
            itemSpec.accept(item);
            items.add(item.build());
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
        private final Map<String, String> headers = new java.util.LinkedHashMap<>();
        private List<ObjectNode> items = new ArrayList<>();
        private String transactionMode;

        TestBulkDeleteBuilder(String entity) {
            this.entity = entity;
        }

        public TestBulkDeleteBuilder items(List<ObjectNode> items) {
            this.items = items;
            return this;
        }

        /**
         * Appends one key, built via key chaining, to the bulk request.
         */
        public TestBulkDeleteBuilder item(Consumer<TestItemBuilder> itemSpec) {
            TestItemBuilder item = new TestItemBuilder();
            itemSpec.accept(item);
            items.add(item.build());
            return this;
        }

        /**
         * Sets a request header (e.g. {@code If-Match}, which the server rejects on a multi-item batch).
         */
        public TestBulkDeleteBuilder header(String name, String value) {
            headers.put(name, value);
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
                return SpeedyTest.this.execute(url, "DELETE", mapper.writeValueAsString(array), headers);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize deleteMany body", e);
            }
        }
    }

    public class TestBulkUpdateBuilder {
        private final String entity;
        private final Map<String, String> headers = new java.util.LinkedHashMap<>();
        private List<ObjectNode> items = new ArrayList<>();
        private String transactionMode;

        TestBulkUpdateBuilder(String entity) {
            this.entity = entity;
        }

        public TestBulkUpdateBuilder items(List<ObjectNode> items) {
            this.items = items;
            return this;
        }

        /**
         * Appends one item, built via key/field chaining, to the bulk request.
         */
        public TestBulkUpdateBuilder item(Consumer<TestItemBuilder> itemSpec) {
            TestItemBuilder item = new TestItemBuilder();
            itemSpec.accept(item);
            items.add(item.build());
            return this;
        }

        /**
         * Sets a request header (e.g. {@code If-Match}, which the server rejects on a multi-item batch).
         */
        public TestBulkUpdateBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public TestBulkUpdateBuilder transaction(String mode) {
            this.transactionMode = mode;
            return this;
        }

        public SpeedyTestResult execute() {
            String url = paths.updatePath(entity);
            if (transactionMode != null && !transactionMode.isEmpty()) {
                url += "?$transaction=" + transactionMode;
            }
            ArrayNode array = mapper.createArrayNode();
            items.forEach(array::add);
            try {
                return SpeedyTest.this.execute(url, "PATCH", mapper.writeValueAsString(array), headers);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize updateMany body", e);
            }
        }
    }

    public class TestBulkReplaceBuilder {
        private final String entity;
        private List<ObjectNode> items = new ArrayList<>();
        private String transactionMode;

        TestBulkReplaceBuilder(String entity) {
            this.entity = entity;
        }

        public TestBulkReplaceBuilder items(List<ObjectNode> items) {
            this.items = items;
            return this;
        }

        /**
         * Appends one item, built via key/field chaining, to the bulk request.
         */
        public TestBulkReplaceBuilder item(Consumer<TestItemBuilder> itemSpec) {
            TestItemBuilder item = new TestItemBuilder();
            itemSpec.accept(item);
            items.add(item.build());
            return this;
        }

        public TestBulkReplaceBuilder transaction(String mode) {
            this.transactionMode = mode;
            return this;
        }

        public SpeedyTestResult execute() {
            String url = paths.updatePath(entity);
            if (transactionMode != null && !transactionMode.isEmpty()) {
                url += "?$transaction=" + transactionMode;
            }
            ArrayNode array = mapper.createArrayNode();
            items.forEach(array::add);
            try {
                return SpeedyTest.this.execute(url, "PUT", mapper.writeValueAsString(array));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize replaceMany body", e);
            }
        }
    }
}
