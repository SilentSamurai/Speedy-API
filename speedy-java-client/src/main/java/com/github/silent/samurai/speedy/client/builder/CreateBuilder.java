package com.github.silent.samurai.speedy.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.exception.SpeedyConnectionException;
import com.github.silent.samurai.speedy.client.exception.SpeedyException;
import com.github.silent.samurai.speedy.client.internal.FieldUtil;
import com.github.silent.samurai.speedy.client.internal.PathBuilder;
import com.github.silent.samurai.speedy.client.internal.RequestSender;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;

import java.io.IOException;
import java.util.Collections;

/**
 * Fluent builder for creating entities via the Speedy API.
 *
 * <pre>{@code
 * User user = speedy.create("User")
 *     .field("name", "Alice")
 *     .field("address.city", "Seattle")
 *     .execute()
 *     .first(User.class);
 * }</pre>
 */
public class CreateBuilder {

    private final String entity;
    private final ObjectNode body;
    private final PathBuilder paths;
    private final RequestSender sender;
    private final ObjectMapper mapper;
    private final ResponseParser parser;

    public CreateBuilder(String entity, PathBuilder paths, RequestSender sender,
                         ObjectMapper mapper, ResponseParser parser) {
        this.entity = entity;
        this.paths = paths;
        this.sender = sender;
        this.mapper = mapper;
        this.parser = parser;
        this.body = mapper.createObjectNode();
    }

    /**
     * Sets a field value. Supports dot-notation for nested/FK fields.
     */
    public CreateBuilder field(String name, Object value) {
        FieldUtil.setField(body, name, value);
        return this;
    }

    /**
     * Builds the JSON request body without executing.
     */
    public ObjectNode build() {
        return body.deepCopy();
    }

    /**
     * Executes the create request and returns the result.
     *
     * @return SpeedyResult containing the created entity(s)
     * @throws SpeedyException on HTTP errors
     * @throws SpeedyConnectionException on network failures
     */
    public SpeedyResult execute() {
        String url = paths.createPath(entity);
        String jsonBody;
        try {
            ArrayNode array = mapper.createArrayNode();
            array.add(body);
            jsonBody = mapper.writeValueAsString(array);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
        SpeedyRequest request = new SpeedyRequest("POST", url, Collections.emptyMap(), jsonBody);
        try {
            SpeedyRawResponse response = sender.send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("Create request failed: " + e.getMessage(), e);
        }
    }
}
