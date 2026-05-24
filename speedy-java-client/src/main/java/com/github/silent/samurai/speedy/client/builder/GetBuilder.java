package com.github.silent.samurai.speedy.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Collections;

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
    private final PathBuilder paths;
    private final RequestSender sender;
    private final ObjectMapper mapper;
    private final ResponseParser parser;

    public GetBuilder(String entity, PathBuilder paths, RequestSender sender,
                      ObjectMapper mapper, ResponseParser parser) {
        this.entity = entity;
        this.paths = paths;
        this.sender = sender;
        this.mapper = mapper;
        this.parser = parser;
        this.pkNode = mapper.createObjectNode();
    }

    /**
     * Sets a primary key field for the lookup.
     */
    public GetBuilder key(String field, Object value) {
        FieldUtil.setField(pkNode, field, value);
        return this;
    }

    /**
     * Executes the get request and returns the result.
     *
     * @return SpeedyResult containing the matched entity(s)
     * @throws SpeedyException on HTTP errors
     * @throws SpeedyConnectionException on network failures
     */
    public SpeedyResult execute() {
        String url = paths.entityPath(entity);
        String queryString = paths.formatPk(pkNode);
        if (queryString != null && !queryString.isEmpty()) {
            url = url + "?" + queryString;
        }
        SpeedyRequest request = new SpeedyRequest("GET", url, Collections.emptyMap(), null);
        try {
            SpeedyRawResponse response = sender.send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("Get request failed: " + e.getMessage(), e);
        }
    }
}
