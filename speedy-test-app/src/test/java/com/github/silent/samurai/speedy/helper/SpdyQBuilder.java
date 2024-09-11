package com.github.silent.samurai.speedy.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.util.IllegalFormatCodePointException;
import java.util.List;

public class SpdyQBuilder {

    private ObjectNode root = CommonUtil.json().createObjectNode();
    private ObjectNode where = CommonUtil.json().createObjectNode();
    private ObjectNode orderBy = CommonUtil.json().createObjectNode();
    private ArrayNode expand = CommonUtil.json().createArrayNode();
    int pageNo = 1;
    int pageSize = 10;

    public SpdyQBuilder $from(String from) {
        root.put("$from", from);
        return this;
    }

    public SpdyQBuilder $where(String key, JsonNode value) {
        where.put(key, value);
        return this;
    }

    public SpdyQBuilder $orderByAsc(String key) {
        orderBy.put(key, "ASC");
        return this;
    }

    public SpdyQBuilder $orderByDesc(String key) {
        orderBy.put(key, "DESC");
        return this;
    }

    public SpdyQBuilder $expand(String key) {
        expand.add(key);
        return this;
    }

    public SpdyQBuilder $pageNo(int pageNo) {
        this.pageNo = pageNo;
        return this;
    }

    public SpdyQBuilder $pageSize(int pageSize) {
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

    public static ObjectNode $ge(Object value) throws JsonProcessingException {
        return toJsonNode(value, "$ge");
    }

    public static ObjectNode $le(Object value) throws JsonProcessingException {
        return toJsonNode(value, "$le");
    }

    public static ObjectNode $in(Object value) throws JsonProcessingException {
        return toJsonNode(value, "$in");
    }

    public static ObjectNode $nin(Object value) throws JsonProcessingException {
        return toJsonNode(value, "$nin");
    }

    public static JsonNode $whereCond(String key, JsonNode value) {
        ObjectNode jsonNodes = CommonUtil.json().createObjectNode();
        jsonNodes.put(key, value);
        return jsonNodes;
    }

    public SpdyQBuilder $and(JsonNode value) throws JsonProcessingException {
        if (!where.has("$and")) {
            where.put("$and", CommonUtil.json().createArrayNode());
        }
        ArrayNode and = (ArrayNode) where.get("$and");
        and.add(value);
        return this;
    }

    public SpdyQBuilder $or(JsonNode value) throws JsonProcessingException {
        if (!where.has("$or")) {
            where.put("$or", CommonUtil.json().createArrayNode());
        }
        ArrayNode or = (ArrayNode) where.get("$or");
        or.add(value);
        return this;
    }

    public JsonNode build() throws IllegalFormatCodePointException {
        root.put("$where", where);
        root.put("$orderBy", orderBy);
        root.put("$expand", expand);
        ObjectNode pageNode = CommonUtil.json().createObjectNode();
        pageNode.put("$pageNo", pageNo);
        pageNode.put("$pageSize", pageSize);
        root.put("$page", pageNode);
        return root;
    }


}
