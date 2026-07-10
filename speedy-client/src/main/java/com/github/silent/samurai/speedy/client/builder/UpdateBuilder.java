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
 * Fluent builder for updating entities via the Speedy API.
 *
 * <pre>{@code
 * speedy.update("User")
 *     .key("id", 123)
 *     .field("name", "Bob")
 *     .execute();
 * }</pre>
 */
public class UpdateBuilder {

    private final String entity;
    private final ObjectNode body;
    private final ObjectNode pkNode;
    private final PathBuilder paths;
    private final RequestSender sender;
    private final ObjectMapper mapper;
    private final ResponseParser parser;
    private final SpeedyFormat format;

    public UpdateBuilder(String entity, PathBuilder paths, RequestSender sender,
                         ObjectMapper mapper, ResponseParser parser) {
        this(entity, paths, sender, mapper, parser, new JsonFormat(mapper));
    }

    public UpdateBuilder(String entity, PathBuilder paths, RequestSender sender,
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
     * Sets the primary key for the entity to update.
     */
    public UpdateBuilder key(String field, Object value) {
        FieldUtil.setField(pkNode, field, value);
        return this;
    }

    /**
     * Sets a field value to update. Supports dot-notation for nested/FK fields.
     */
    public UpdateBuilder field(String name, Object value) {
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
     * Executes the update request and returns the result.
     *
     * @return SpeedyResult containing the updated entity
     * @throws SpeedyException           on HTTP errors
     * @throws SpeedyConnectionException on network failures
     */
    public SpeedyResult execute() {
        String url = paths.updatePath(entity);
        if (!pkNode.isEmpty()) {
            body.setAll(pkNode);
        }
        String requestBody = format.write(body);
        SpeedyRequest request = new SpeedyRequest("PATCH", url, FormatHeaders.forBody(format), requestBody);
        try {
            SpeedyRawResponse response = sender.send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("Update request failed: " + e.getMessage(), e);
        }
    }
}
