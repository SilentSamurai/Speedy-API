package com.github.silent.samurai.speedy.client.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.exception.SpeedyConnectionException;
import com.github.silent.samurai.speedy.client.exception.SpeedyException;
import com.github.silent.samurai.speedy.client.internal.FieldUtil;
import com.github.silent.samurai.speedy.client.internal.PathBuilder;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;
import com.github.silent.samurai.speedy.client.internal.RequestSender;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * Fluent builder for executing advanced queries against the Speedy API.
 *
 * <pre>{@code
 * import static com.github.silent.samurai.speedy.client.SpeedyQuery.*;
 *
 * List<User> users = speedy.query("User")
 *     .where(condition("active", eq(true)))
 *     .orderByAsc("name")
 *     .pageSize(20)
 *     .execute()
 *     .list(User.class);
 *
 * long count = speedy.query("User")
 *     .where(condition("active", eq(true)))
 *     .count();
 * }</pre>
 */
public class QueryBuilder {

    private final String entity;
    private final ObjectNode body;
    private final PathBuilder paths;
    private final RequestSender sender;
    private final ObjectMapper mapper;
    private final ResponseParser parser;

    public QueryBuilder(String entity, PathBuilder paths, RequestSender sender,
                        ObjectMapper mapper, ResponseParser parser) {
        this.entity = entity;
        this.paths = paths;
        this.sender = sender;
        this.mapper = mapper;
        this.parser = parser;
        this.body = mapper.createObjectNode();
        this.body.put("$from", entity);
    }

    /**
     * Sets WHERE conditions for the query.
     */
    public QueryBuilder where(JsonNode... conditions) {
        if (conditions == null || conditions.length == 0) {
            return this;
        }
        for (JsonNode condition : conditions) {
            if (condition.has("$and") || condition.has("$or")) {
                body.remove("$where");
                body.setAll((ObjectNode) condition);
                return this;
            }
            if (!body.has("$where")) {
                body.set("$where", mapper.createObjectNode());
            }
            ObjectNode whereNode = (ObjectNode) body.get("$where");
            condition.fields().forEachRemaining(entry -> whereNode.set(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    /**
     * Sets ascending order for the given field.
     */
    public QueryBuilder orderByAsc(String field) {
        if (!body.has("$orderBy")) {
            body.set("$orderBy", mapper.createObjectNode());
        }
        ((ObjectNode) body.get("$orderBy")).put(field, "ASC");
        return this;
    }

    /**
     * Sets descending order for the given field.
     */
    public QueryBuilder orderByDesc(String field) {
        if (!body.has("$orderBy")) {
            body.set("$orderBy", mapper.createObjectNode());
        }
        ((ObjectNode) body.get("$orderBy")).put(field, "DESC");
        return this;
    }

    /**
     * Sets the page number (0-based).
     */
    public QueryBuilder pageNo(int pageNo) {
        ObjectNode pageNode;
        if (!body.has("$page")) {
            pageNode = mapper.createObjectNode();
            body.set("$page", pageNode);
        } else {
            pageNode = (ObjectNode) body.get("$page");
        }
        pageNode.put("$index", pageNo);
        return this;
    }

    /**
     * Sets the page size (number of items per page).
     */
    public QueryBuilder pageSize(int pageSize) {
        ObjectNode pageNode;
        if (!body.has("$page")) {
            pageNode = mapper.createObjectNode();
            body.set("$page", pageNode);
        } else {
            pageNode = (ObjectNode) body.get("$page");
        }
        pageNode.put("$size", pageSize);
        return this;
    }

    /**
     * Specifies which fields to include in the response (projection).
     */
    public QueryBuilder select(String... fields) {
        ArrayNode selectArray;
        if (!body.has("$select")) {
            selectArray = mapper.createArrayNode();
            body.set("$select", selectArray);
        } else {
            selectArray = (ArrayNode) body.get("$select");
        }
        for (String field : fields) {
            selectArray.add(field);
        }
        return this;
    }

    /**
     * Specifies which related entities to expand/include in the response.
     */
    public QueryBuilder expand(String... relations) {
        ArrayNode expandArray;
        if (!body.has("$expand")) {
            expandArray = mapper.createArrayNode();
            body.set("$expand", expandArray);
        } else {
            expandArray = (ArrayNode) body.get("$expand");
        }
        for (String relation : relations) {
            expandArray.add(relation);
        }
        return this;
    }

    /**
     * Builds the JSON query body without executing.
     */
    public JsonNode build() {
        return body.deepCopy();
    }

    /**
     * Executes the query and returns entity results.
     *
     * @return SpeedyResult containing the matched entities
     * @throws SpeedyException on HTTP errors
     * @throws SpeedyConnectionException on network failures
     */
    public SpeedyResult execute() {
        String url = paths.queryPath(entity);
        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize query body", e);
        }
        SpeedyRequest request = new SpeedyRequest("POST", url, Collections.emptyMap(), jsonBody);
        try {
            SpeedyRawResponse response = sender.send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("Query request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a count query and returns the count.
     *
     * @return the number of matching entities
     * @throws SpeedyException on HTTP errors
     * @throws SpeedyConnectionException on network failures
     */
    public long count() {
        String url = paths.countPath(entity);
        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize query body", e);
        }
        SpeedyRequest request = new SpeedyRequest("POST", url, Collections.emptyMap(), jsonBody);
        try {
            SpeedyRawResponse response = sender.send(request);
            return parser.parseCountResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("Count request failed: " + e.getMessage(), e);
        }
    }
}
