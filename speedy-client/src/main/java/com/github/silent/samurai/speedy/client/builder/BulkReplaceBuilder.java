package com.github.silent.samurai.speedy.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.exception.SpeedyConnectionException;
import com.github.silent.samurai.speedy.client.format.JsonFormat;
import com.github.silent.samurai.speedy.client.format.SpeedyFormat;
import com.github.silent.samurai.speedy.client.internal.FormatHeaders;
import com.github.silent.samurai.speedy.client.internal.PathBuilder;
import com.github.silent.samurai.speedy.client.internal.RequestSender;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;

import java.io.IOException;
import java.util.List;

/**
 * Fluent builder for bulk-replacing entities via the Speedy API (HTTP {@code PUT}).
 * Each item is a full-replace body (key + complete representation), identical in shape to a
 * single {@link ReplaceBuilder} body. Contrast with {@link BulkUpdateBuilder}, which issues a
 * {@code PATCH} (partial update) per item.
 */
public class BulkReplaceBuilder {

    private final String entity;
    private final PathBuilder paths;
    private final RequestSender sender;
    private final ObjectMapper mapper;
    private final ResponseParser parser;
    private final SpeedyFormat format;
    private List<ObjectNode> items;
    private String transactionMode;

    public BulkReplaceBuilder(String entity, PathBuilder paths, RequestSender sender,
                              ObjectMapper mapper, ResponseParser parser) {
        this(entity, paths, sender, mapper, parser, new JsonFormat(mapper));
    }

    public BulkReplaceBuilder(String entity, PathBuilder paths, RequestSender sender,
                              ObjectMapper mapper, ResponseParser parser, SpeedyFormat format) {
        this.entity = entity;
        this.paths = paths;
        this.sender = sender;
        this.mapper = mapper;
        this.parser = parser;
        this.format = format;
    }

    public BulkReplaceBuilder items(List<ObjectNode> items) {
        this.items = items;
        return this;
    }

    public BulkReplaceBuilder transaction(String mode) {
        this.transactionMode = mode;
        return this;
    }

    public SpeedyResult execute() {
        if (items == null) {
            throw new IllegalStateException("items is required");
        }
        String url = paths.updatePath(entity);
        if (transactionMode != null && !transactionMode.isEmpty()) {
            url += "?$transaction=" + transactionMode;
        }
        ArrayNode array = mapper.createArrayNode();
        for (ObjectNode entityNode : items) {
            array.add(entityNode);
        }
        String requestBody = format.write(array);
        SpeedyRequest request = new SpeedyRequest("PUT", url, FormatHeaders.forBody(format), requestBody);
        try {
            SpeedyRawResponse response = sender.send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("ReplaceMany request failed: " + e.getMessage(), e);
        }
    }
}
