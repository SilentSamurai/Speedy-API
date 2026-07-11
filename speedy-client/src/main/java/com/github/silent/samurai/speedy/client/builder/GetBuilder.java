package com.github.silent.samurai.speedy.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.exception.SpeedyConnectionException;
import com.github.silent.samurai.speedy.client.exception.SpeedyException;
import com.github.silent.samurai.speedy.client.format.JsonFormat;
import com.github.silent.samurai.speedy.client.format.SpeedyFormat;
import com.github.silent.samurai.speedy.client.internal.FieldUtil;
import com.github.silent.samurai.speedy.client.internal.FormatHeaders;
import com.github.silent.samurai.speedy.client.internal.PathBuilder;
import com.github.silent.samurai.speedy.client.internal.RequestSender;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;

import java.io.IOException;
import java.util.*;

/**
 * Fluent builder for fetching entities by primary key via the Speedy API.
 *
 * <pre>{@code
 * User user = speedy.get("User")
 *     .key("id", 123)
 *     .execute()
 *     .first(User.class);
 * }</pre>
 */
public class GetBuilder {

    private final String entity;
    private final ObjectNode pkNode;
    private final List<String> selectFields;
    private final List<String> expandRelations;
    private final PathBuilder paths;
    private final RequestSender sender;
    private final ResponseParser parser;
    private final SpeedyFormat format;
    private Integer pageSize;
    private Integer pageNo;
    private String deleted;

    public GetBuilder(String entity, PathBuilder paths, RequestSender sender,
                      ObjectMapper mapper, ResponseParser parser) {
        this(entity, paths, sender, mapper, parser, new JsonFormat(mapper));
    }

    public GetBuilder(String entity, PathBuilder paths, RequestSender sender,
                      ObjectMapper mapper, ResponseParser parser, SpeedyFormat format) {
        this.entity = entity;
        this.paths = paths;
        this.sender = sender;
        this.parser = parser;
        this.format = format;
        this.pkNode = mapper.createObjectNode();
        this.selectFields = new ArrayList<>();
        this.expandRelations = new ArrayList<>();
    }

    /**
     * Sets a primary key field for the lookup.
     */
    public GetBuilder key(String field, Object value) {
        FieldUtil.setField(pkNode, field, value);
        return this;
    }

    /**
     * Specifies which fields to include in the response (projection).
     */
    public GetBuilder select(String... fields) {
        selectFields.addAll(Arrays.asList(fields));
        return this;
    }

    /**
     * Sets the page size (number of items per page).
     */
    public GetBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Sets the page number (0-based).
     */
    public GetBuilder pageNo(int pageNo) {
        this.pageNo = pageNo;
        return this;
    }

    /**
     * Specifies which related entities to expand/include in the response.
     */
    public GetBuilder expand(String... relations) {
        expandRelations.addAll(Arrays.asList(relations));
        return this;
    }

    /**
     * Controls how soft-deleted rows are treated: {@code "exclude"} (default), {@code "include"}
     * (live + deleted), or {@code "only"} (recycle-bin view). {@code include}/{@code only} require
     * the entity to allow viewing deleted rows.
     */
    public GetBuilder deleted(String mode) {
        this.deleted = mode;
        return this;
    }

    /**
     * Includes soft-deleted rows alongside live ones ({@code $deleted=include}).
     */
    public GetBuilder includeDeleted() {
        return deleted("include");
    }

    /**
     * Returns only soft-deleted rows ({@code $deleted=only}) — a recycle-bin view.
     */
    public GetBuilder onlyDeleted() {
        return deleted("only");
    }

    /**
     * Executes the get request and returns the result.
     *
     * @return SpeedyResult containing the matched entity(s)
     * @throws SpeedyException           on HTTP errors
     * @throws SpeedyConnectionException on network failures
     */
    public SpeedyResult execute() {
        String url = paths.entityPath(entity);
        String queryString = buildQueryString();
        if (!queryString.isEmpty()) {
            url = url + "?" + queryString;
        }
        SpeedyRequest request = new SpeedyRequest("GET", url, FormatHeaders.acceptOnly(format), null);
        try {
            SpeedyRawResponse response = sender.send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("Get request failed: " + e.getMessage(), e);
        }
    }

    private String buildQueryString() {
        StringBuilder sb = new StringBuilder();

        Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = pkNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
            if (!sb.isEmpty()) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue().asText());
        }

        if (!selectFields.isEmpty()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append("$select=").append(String.join(",", selectFields));
        }

        if (pageSize != null) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append("$pageSize=").append(pageSize);
        }

        if (pageNo != null) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append("$pageNo=").append(pageNo);
        }

        if (!expandRelations.isEmpty()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append("$expand=").append(String.join(",", expandRelations));
        }

        if (deleted != null) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append("$deleted=").append(deleted);
        }

        return sb.toString();
    }
}
