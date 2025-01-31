package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SpeedyQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyQuery.class);

    private final ObjectNode root = CommonUtil.json().createObjectNode();
    private final ObjectNode where = CommonUtil.json().createObjectNode();
    private final ObjectNode orderBy = CommonUtil.json().createObjectNode();
    private final ArrayNode expand = CommonUtil.json().createArrayNode();
    private final Set<String> select = new HashSet<>();
    int pageNo = 1;
    int pageSize = 10;

    private SpeedyQuery() {
    }

    public static SpeedyQuery builder(String entity) {
        return new SpeedyQuery().$from(entity);
    }

    public static SpeedyQuery builder() {
        return new SpeedyQuery();
    }


    public SpeedyQuery $from(String from) {
        if (from == null || from.isEmpty()) {
            throw new IllegalArgumentException("The 'from' parameter cannot be null or empty.");
        }
        root.put("$from", from);
        return this;
    }

    public SpeedyQuery $where(JsonNode... conditionObjs) {
        for (JsonNode conditionObj : conditionObjs) {
            if (conditionObj == null || conditionObj.isEmpty()) {
                throw new IllegalArgumentException("The 'where' parameter cannot be null or empty.");
            }
            if (conditionObj.has("$and")) {
                where.removeAll();
                where.set("$and", conditionObj.get("$and"));
                break;
            }
            if (conditionObj.has("$or")) {
                where.removeAll();
                where.set("$or", conditionObj.get("$or"));
                break;
            }
            if (!conditionObj.isEmpty()) {
                String firstField = conditionObj.fieldNames().next();
                where.set(firstField, conditionObj.get(firstField));
            }
        }
        return this;
    }

//    public SpdyQBuilder $whereCondition(String key, JsonNode value) {
//        Objects.requireNonNull(key, "Key must not be null");
//        where.set(key, value);
//        return this;
//    }

    public SpeedyQuery $orderByAsc(String key) {
        Objects.requireNonNull(key, "Key must not be null");
        orderBy.set(key, new TextNode("ASC"));
        return this;
    }

    public SpeedyQuery $orderByDesc(String key) {
        orderBy.set(key, new TextNode("DESC"));
        return this;
    }

    public SpeedyQuery $expand(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Expand key cannot be null or empty.");
        }
        expand.add(key);
        return this;
    }

    public SpeedyQuery $pageNo(int pageNo) {
        if (pageNo < 0) {
            throw new IllegalArgumentException("Page number must not be less than 0.");
        }
        this.pageNo = pageNo;
        return this;
    }

    public SpeedyQuery $pageSize(int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0.");
        }
        this.pageSize = pageSize;
        return this;
    }

    private static ObjectNode toJsonNode(Object value, String condition) throws JsonProcessingException {
        ObjectNode jsonNodes = CommonUtil.json().createObjectNode();
        JsonNode jsonNode = CommonUtil.json().convertValue(value, JsonNode.class);
        jsonNodes.set(condition, jsonNode);
        return jsonNodes;
    }

    public static ObjectNode $eq(Object value) throws JsonProcessingException {
        return toJsonNode(value, "$eq");
    }

    public static ObjectNode $ne(Object value) throws JsonProcessingException {
        return toJsonNode(value, "$ne");
    }

    public static ObjectNode $gt(Object value) throws JsonProcessingException {
        return toJsonNode(value, "$gt");
    }

    public static ObjectNode $lt(Object value) throws JsonProcessingException {
        return toJsonNode(value, "$lt");
    }

    public static ObjectNode $gte(Object value) throws JsonProcessingException {
        return toJsonNode(value, "$gte");
    }

    public static ObjectNode $lte(Object value) throws JsonProcessingException {
        return toJsonNode(value, "$lte");
    }

    public static ObjectNode $in(Object... values) throws JsonProcessingException {
        return toJsonNode(values, "$in");
    }

    public static ObjectNode $nin(Object... values) throws JsonProcessingException {
        return toJsonNode(values, "$nin");
    }

    public static JsonNode $condition(String key, JsonNode value) {
        ObjectNode jsonNodes = CommonUtil.json().createObjectNode();
        jsonNodes.set(key, value);
        return jsonNodes;
    }

    public static ObjectNode $or(JsonNode... conditions) {
        ObjectNode andNode = CommonUtil.json().createObjectNode();
        andNode.set("$or", CommonUtil.json().createArrayNode());
        for (JsonNode condition : conditions) {
            ArrayNode and = (ArrayNode) andNode.get("$or");
            and.add(condition);
        }
        return andNode;
    }

    public static ObjectNode $and(JsonNode... conditions) {
        ObjectNode andNode = CommonUtil.json().createObjectNode();
        andNode.set("$and", CommonUtil.json().createArrayNode());
        for (JsonNode condition : conditions) {
            ArrayNode and = (ArrayNode) andNode.get("$and");
            and.add(condition);
        }
        return andNode;
    }

//    public SpdyQBuilder $and(JsonNode value) throws JsonProcessingException {
//        if (!where.has("$and")) {
//            where.set("$and", CommonUtil.json().createArrayNode());
//        }
//        ArrayNode and = (ArrayNode) where.get("$and");
//        and.add(value);
//        return this;
//    }
//
//    public SpdyQBuilder $or(JsonNode value) throws JsonProcessingException {
//        if (!where.has("$or")) {
//            where.set("$or", CommonUtil.json().createArrayNode());
//        }
//        ArrayNode or = (ArrayNode) where.get("$or");
//        or.add(value);
//        return this;
//    }

    public JsonNode build() throws IllegalFormatCodePointException {
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
            ArrayNode arrayNode = CommonUtil.json().createArrayNode();
            select.forEach(arrayNode::add);
            root.set("$select", arrayNode);
        }
        ObjectNode pageNode = CommonUtil.json().createObjectNode();
        pageNode.put("$pageNo", pageNo);
        pageNode.put("$pageSize", pageSize);
        root.set("$page", pageNode);
        return root;
    }

    public SpeedyQuery prettyPrint() throws JsonProcessingException {
        ObjectMapper json = CommonUtil.json();// Using the existing ObjectMapper instance

        // Create a copy of the root node to avoid modifying the original
        ObjectNode rootCopy = root.deepCopy();
        rootCopy.set("$where", where);
        rootCopy.set("$orderBy", orderBy);
        rootCopy.set("$expand", expand);

        // Page information
        ObjectNode pageNode = json.createObjectNode();
        pageNode.put("$pageNo", pageNo);
        pageNode.put("$pageSize", pageSize);
        rootCopy.set("$page", pageNode);

        // Return the pretty-printed JSON string
        String output = json.writerWithDefaultPrettyPrinter().writeValueAsString(rootCopy);
        LOGGER.info("SpeedyQuery: {}", output);
        return this;
    }

    public String getFrom() {
        return root.get("$from").asText();
    }

    public SpeedyQuery $select(String... select) {
        this.select.addAll(Arrays.asList(select));
        return this;
    }
}
