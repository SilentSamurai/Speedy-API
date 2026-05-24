package com.github.silent.samurai.speedy.client.internal;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

/**
 * Constructs API endpoint URLs from base URL, API path, entity name, and operation suffix.
 * Used by all builder classes to generate the correct REST endpoint.
 */
public class PathBuilder {

    private final String baseUrl;
    private final String apiPath;

    public PathBuilder(String baseUrl, String apiPath) {
        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = apiPath.startsWith("/") ? apiPath : "/" + apiPath;
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        this.baseUrl = url;
        this.apiPath = path;
    }

    public String entityPath(String entity) {
        return baseUrl + apiPath + entity;
    }

    public String createPath(String entity) {
        return entityPath(entity) + "/$create";
    }

    public String updatePath(String entity) {
        return entityPath(entity) + "/$update";
    }

    public String deletePath(String entity) {
        return entityPath(entity) + "/$delete";
    }

    public String queryPath(String entity) {
        return entityPath(entity) + "/$query";
    }

    public String countPath(String entity) {
        return entityPath(entity) + "/$count";
    }

    public String metadataPath() {
        return baseUrl + apiPath + "$metadata";
    }

    public String formatPk(ObjectNode pkNode) {
        if (pkNode == null || pkNode.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = pkNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue().asText());
        }
        return sb.toString();
    }
}
