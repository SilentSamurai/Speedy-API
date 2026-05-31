package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Fluent query builder for constructing JSON query bodies for the Speedy API.
 *
 * <p>All operator methods ({@code eq}, {@code ne}, {@code gt}, etc.) are static
 * factory methods returning {@link ObjectNode}. The instance methods build up
 * the full query JSON including WHERE, ORDER BY, SELECT, EXPAND, and pagination.
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * import static com.github.silent.samurai.speedy.client.SpeedyQuery.*;
 *
 * SpeedyQuery query = from("users")
 *     .where(
 *         and(
 *             condition("active", eq(true)),
 *             condition("age", gte(18))
 *         )
 *     )
 *     .select("id", "name", "email")
 *     .orderByAsc("name")
 *     .pageSize(20)
 *     .build();
 * }</pre>
 */
public class SpeedyQuery {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ObjectNode root = MAPPER.createObjectNode();
    private final ObjectNode where = MAPPER.createObjectNode();
    private final ObjectNode orderBy = MAPPER.createObjectNode();
    private final ArrayNode expand = MAPPER.createArrayNode();
    private final Set<String> select = new LinkedHashSet<>();
    int pageNo = 0;
    int pageSize = 10;

    public static SpeedyQuery from(String entity) {
        return new SpeedyQuery().fromEntity(entity);
    }

    public static SpeedyQuery from() {
        return new SpeedyQuery();
    }

    private static ObjectNode toJsonNode(Object value, String condition) {
        ObjectNode jsonNodes = MAPPER.createObjectNode();
        JsonNode jsonNode = MAPPER.convertValue(value, JsonNode.class);
        jsonNodes.set(condition, jsonNode);
        return jsonNodes;
    }

    public static ObjectNode eq(Object value) {
        return toJsonNode(value, "$eq");
    }

    public static ObjectNode ne(Object value) {
        return toJsonNode(value, "$ne");
    }

    public static ObjectNode gt(Object value) {
        return toJsonNode(value, "$gt");
    }

    public static ObjectNode lt(Object value) {
        return toJsonNode(value, "$lt");
    }

    public static ObjectNode gte(Object value) {
        return toJsonNode(value, "$gte");
    }

    public static ObjectNode lte(Object value) {
        return toJsonNode(value, "$lte");
    }

    public static ObjectNode in(Object... values) {
        return toJsonNode(values, "$in");
    }

    public static ObjectNode nin(Object... values) {
        return toJsonNode(values, "$nin");
    }

    public static ObjectNode matches(Object values) {
        return toJsonNode(values, "$matches");
    }

    public static ObjectNode contains(Object values) {
        return toJsonNode(values, "$contains");
    }

    /**
     * Creates a {@code $between} value node for a range query.
     * Generates an equivalent {@code field &gt;= low AND field &lt;= high}.
     */
    public static ObjectNode between(Object low, Object high) {
        return toJsonNode(new Object[]{low, high}, "$between");
    }

    /**
     * Creates a {@code $isnull} value node for an IS NULL query.
     */
    public static ObjectNode isnull() {
        return toJsonNode(true, "$isnull");
    }

    /**
     * Creates a {@code $isnotnull} value node for an IS NOT NULL query.
     */
    public static ObjectNode isnotnull() {
        return toJsonNode(true, "$isnotnull");
    }

    public static JsonNode condition(String key, JsonNode value) {
        ObjectNode jsonNodes = MAPPER.createObjectNode();
        jsonNodes.set(key, value);
        return jsonNodes;
    }

    public static ObjectNode or(JsonNode... conditions) {
        ObjectNode orNode = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();
        for (JsonNode condition : conditions) {
            array.add(condition);
        }
        orNode.set("$or", array);
        return orNode;
    }

    public static ObjectNode and(JsonNode... conditions) {
        ObjectNode andNode = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();
        for (JsonNode condition : conditions) {
            array.add(condition);
        }
        andNode.set("$and", array);
        return andNode;
    }

    public SpeedyQuery fromEntity(String from) {
        if (from == null || from.isEmpty()) {
            throw new IllegalArgumentException("The 'from' parameter cannot be null or empty.");
        }
        root.put("$from", from);
        return this;
    }

    public SpeedyQuery where(JsonNode... conditionObjs) {
        where.removeAll();
        for (JsonNode conditionObj : conditionObjs) {
            if (conditionObj == null || conditionObj.isEmpty()) {
                throw new IllegalArgumentException("The 'where' parameter cannot be null or empty.");
            }
            if (conditionObj.has("$and") || conditionObj.has("$or")) {
                where.removeAll();
                conditionObj.fields().forEachRemaining(entry -> where.set(entry.getKey(), entry.getValue()));
                break;
            }
            String firstField = conditionObj.fieldNames().next();
            where.set(firstField, conditionObj.get(firstField));
        }
        return this;
    }

    public SpeedyQuery orderByAsc(String key) {
        Objects.requireNonNull(key, "Key must not be null");
        orderBy.set(key, new TextNode("ASC"));
        return this;
    }

    public SpeedyQuery orderByDesc(String key) {
        orderBy.set(key, new TextNode("DESC"));
        return this;
    }

    public SpeedyQuery expand(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Expand key cannot be null or empty.");
        }
        expand.add(key);
        return this;
    }

    public SpeedyQuery pageNo(int pageNo) {
        if (pageNo < 0) {
            throw new IllegalArgumentException("Page number must not be less than 0.");
        }
        this.pageNo = pageNo;
        return this;
    }

    public SpeedyQuery pageSize(int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0.");
        }
        this.pageSize = pageSize;
        return this;
    }

    public JsonNode build() {
        if (!where.isEmpty()) {
            root.set("$where", where);
        }
        if (!orderBy.isEmpty()) {
            root.set("$orderBy", orderBy);
        }
        if (!expand.isEmpty()) {
            root.set("$expand", expand);
        }
        if (!select.isEmpty()) {
            ArrayNode arrayNode = MAPPER.createArrayNode();
            select.forEach(arrayNode::add);
            root.set("$select", arrayNode);
        }
        ObjectNode pageNode = MAPPER.createObjectNode();
        pageNode.put("$index", pageNo);
        pageNode.put("$size", pageSize);
        root.set("$page", pageNode);
        return root;
    }

    public SpeedyQuery select(String... select) {
        this.select.addAll(Arrays.asList(select));
        return this;
    }

    public SpeedyQuery prettyPrint() {
        build();
        return this;
    }

    public String getFrom() {
        return root.get("$from") != null ? root.get("$from").asText() : null;
    }
}
