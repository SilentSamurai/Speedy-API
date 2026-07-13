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

public class BulkCreateBuilder {

    private final String entity;
    private final PathBuilder paths;
    private final RequestSender sender;
    private final ObjectMapper mapper;
    private final ResponseParser parser;
    private final SpeedyFormat format;
    private List<ObjectNode> items;

    public BulkCreateBuilder(String entity, PathBuilder paths, RequestSender sender,
                             ObjectMapper mapper, ResponseParser parser) {
        this(entity, paths, sender, mapper, parser, new JsonFormat(mapper));
    }

    public BulkCreateBuilder(String entity, PathBuilder paths, RequestSender sender,
                             ObjectMapper mapper, ResponseParser parser, SpeedyFormat format) {
        this.entity = entity;
        this.paths = paths;
        this.sender = sender;
        this.mapper = mapper;
        this.parser = parser;
        this.format = format;
    }

    public BulkCreateBuilder items(List<ObjectNode> items) {
        this.items = items;
        return this;
    }

    public SpeedyResult execute() {
        String url = paths.createPath(entity);
        ArrayNode array = mapper.createArrayNode();
        for (ObjectNode entityNode : items) {
            array.add(entityNode);
        }
        String requestBody = format.write(array);
        SpeedyRequest request = new SpeedyRequest("POST", url, FormatHeaders.forBody(format), requestBody);
        try {
            SpeedyRawResponse response = sender.send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("CreateMany request failed: " + e.getMessage(), e);
        }
    }
}
