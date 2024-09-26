package com.github.silent.samurai.speedy.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.Getter;

public class SpeedyCreateRequestBuilder {

    ObjectNode entity = CommonUtil.json().createObjectNode();
    @Getter
    private final String entityName;

    public SpeedyCreateRequestBuilder(String entityName) {
        this.entityName = entityName;
    }

    public SpeedyCreateRequestBuilder addField(String field, Object value) {
        String[] parts = field.split("\\.");
        ObjectNode currentNode = entity;

        // Traverse or create intermediate nodes
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!currentNode.has(part)) {
                currentNode.set(part, CommonUtil.json().createObjectNode());
            }
            currentNode = (ObjectNode) currentNode.get(part);
        }

        // Set the final field value
        currentNode.set(parts[parts.length - 1], CommonUtil.json().convertValue(value, JsonNode.class));

        return this;
    }

    public SpeedyCreateRequest build() {
        SpeedyCreateRequest speedyCreateRequest = new SpeedyCreateRequest();
        speedyCreateRequest.setEntity(entityName);
        speedyCreateRequest.setBody(entity);
        return speedyCreateRequest;
    }


}
