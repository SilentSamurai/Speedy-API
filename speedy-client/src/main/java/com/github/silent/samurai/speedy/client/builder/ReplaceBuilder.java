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

/**
 * Fluent builder for <em>fully replacing</em> an entity via the Speedy API (HTTP {@code PUT}).
 * The payload is the complete representation of the resource: required fields must be present
 * and omitted nullable fields are reset to null. Contrast with {@link UpdateBuilder}, which
 * issues a {@code PATCH} (partial update).
 *
 * <pre>{@code
 * speedy.replace("User")
 *     .key("id", 123)
 *     .field("name", "Bob")
 *     .field("email", "bob@example.com")
 *     .execute();
 * }</pre>
 */
public class ReplaceBuilder {

    private final String entity;
    private final ObjectNode body;
    private final ObjectNode pkNode;
    private final PathBuilder paths;
    private final RequestSender sender;
    private final ObjectMapper mapper;
    private final ResponseParser parser;
    private final SpeedyFormat format;

    public ReplaceBuilder(String entity, PathBuilder paths, RequestSender sender,
                          ObjectMapper mapper, ResponseParser parser) {
        this(entity, paths, sender, mapper, parser, new JsonFormat(mapper));
    }

    public ReplaceBuilder(String entity, PathBuilder paths, RequestSender sender,
                          ObjectMapper mapper, ResponseParser parser, SpeedyFormat format) {
        this.entity = entity;
        this.paths = paths;
        this.sender = sender;
        this.mapper = mapper;
        this.parser = parser;
        this.format = format;
        this.body = mapper.createObjectNode();
        this.pkNode = mapper.createObjectNode();
    }

    /**
     * Sets the primary key for the entity to replace.
     */
    public ReplaceBuilder key(String field, Object value) {
        FieldUtil.setField(pkNode, field, value);
        return this;
    }

    /**
     * Sets a field value. Supports dot-notation for nested/FK fields.
     */
    public ReplaceBuilder field(String name, Object value) {
        FieldUtil.setField(body, name, value);
        return this;
    }

    /**
     * Builds the JSON request body (key + fields merged) without executing.
     */
    public ObjectNode build() {
        if (!pkNode.isEmpty()) {
            body.setAll(pkNode);
        }
        return body;
    }

    /**
     * Executes the replace request and returns the result.
     *
     * @return SpeedyResult containing the replaced entity
     * @throws SpeedyException           on HTTP errors
     * @throws SpeedyConnectionException on network failures
     */
    public SpeedyResult execute() {
        String url = paths.updatePath(entity);
        if (!pkNode.isEmpty()) {
            body.setAll(pkNode);
        }
        String requestBody = format.write(body);
        SpeedyRequest request = new SpeedyRequest("PUT", url, FormatHeaders.forBody(format), requestBody);
        try {
            SpeedyRawResponse response = sender.send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("Replace request failed: " + e.getMessage(), e);
        }
    }
}
