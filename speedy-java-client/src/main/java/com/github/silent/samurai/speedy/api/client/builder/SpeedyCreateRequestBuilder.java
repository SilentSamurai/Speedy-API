package com.github.silent.samurai.speedy.api.client.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.api.client.SpeedyApi;
import com.github.silent.samurai.speedy.api.client.models.SpeedyCreateRequest;
import com.github.silent.samurai.speedy.api.client.utils.JsonUtil;
import lombok.Getter;

public class SpeedyCreateRequestBuilder<T> {

    private final ObjectNode entity = JsonUtil.getObjectMapper().createObjectNode();
    @Getter
    private final String entityName;
    private final SpeedyApi<T> speedyApi;

    public SpeedyCreateRequestBuilder(String entityName, SpeedyApi<T> speedyApi) {
        this.entityName = entityName;
        this.speedyApi = speedyApi;
    }

    /**
     * Add a field with its value to the entity.
     * Supports nested field paths using dot notation (e.g., "profile.name").
     *
     * @param field the field name or path
     * @param value the field value
     * @return this builder instance
     */
    public <R> SpeedyCreateRequestBuilder<T> addField(String field, R value) {
        String[] parts = field.split("\\.");
        ObjectNode currentNode = entity;

        // Traverse or create intermediate nodes
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!currentNode.has(part)) {
                currentNode.set(part, JsonUtil.getObjectMapper().createObjectNode());
            }
            currentNode = (ObjectNode) currentNode.get(part);
        }

        // Set the final field value
        currentNode.set(parts[parts.length - 1], JsonUtil.getObjectMapper().convertValue(value, JsonNode.class));

        return this;
    }

    /**
     * Build the SpeedyCreateRequest without executing it.
     *
     * @return the constructed SpeedyCreateRequest
     */
    private SpeedyCreateRequest build() {
        SpeedyCreateRequest speedyCreateRequest = new SpeedyCreateRequest();
        speedyCreateRequest.setEntity(entityName);
        speedyCreateRequest.setBody(entity);
        return speedyCreateRequest;
    }

    /**
     * Build and execute the creation request using the configured SpeedyApi.
     *
     * @return the response from the API
     */
    public T execute() throws Exception {
        SpeedyCreateRequest request = build();
        return speedyApi.create(request);
    }

}
