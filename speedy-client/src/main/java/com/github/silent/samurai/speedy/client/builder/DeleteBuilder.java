package com.github.silent.samurai.speedy.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
 * Fluent builder for deleting entities by primary key via the Speedy API.
 *
 * <pre>{@code
 * speedy.delete("User")
 *     .key("id", 123)
 *     .execute();
 * }</pre>
 */
public class DeleteBuilder {

    private final String entity;
    private final ObjectNode pkNode;
    private final PathBuilder paths;
    private final RequestSender sender;
    private final ObjectMapper mapper;
    private final ResponseParser parser;
    private final SpeedyFormat format;

    public DeleteBuilder(String entity, PathBuilder paths, RequestSender sender,
                         ObjectMapper mapper, ResponseParser parser) {
        this(entity, paths, sender, mapper, parser, new JsonFormat(mapper));
    }

    public DeleteBuilder(String entity, PathBuilder paths, RequestSender sender,
                         ObjectMapper mapper, ResponseParser parser, SpeedyFormat format) {
        this.entity = entity;
        this.paths = paths;
        this.sender = sender;
        this.mapper = mapper;
        this.parser = parser;
        this.format = format;
        this.pkNode = mapper.createObjectNode();
    }

    /**
     * Sets the primary key for the entity to delete.
     */
    public DeleteBuilder key(String field, Object value) {
        FieldUtil.setField(pkNode, field, value);
        return this;
    }

    /**
     * Executes the delete request and returns the result.
     *
     * @return SpeedyResult containing the deleted entity(s)
     * @throws SpeedyException           on HTTP errors
     * @throws SpeedyConnectionException on network failures
     */
    public SpeedyResult execute() {
        String url = paths.deletePath(entity);
        ArrayNode array = mapper.createArrayNode();
        array.add(pkNode);
        String requestBody = format.write(array);
        SpeedyRequest request = new SpeedyRequest("DELETE", url, FormatHeaders.forBody(format), requestBody);
        try {
            SpeedyRawResponse response = sender.send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("Delete request failed: " + e.getMessage(), e);
        }
    }
}
