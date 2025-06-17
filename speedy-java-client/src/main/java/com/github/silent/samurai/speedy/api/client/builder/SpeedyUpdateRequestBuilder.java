package com.github.silent.samurai.speedy.api.client.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.api.client.SpeedyApi;
import com.github.silent.samurai.speedy.api.client.models.SpeedyUpdateRequest;
import com.github.silent.samurai.speedy.api.client.utils.JsonUtil;
import lombok.Getter;

public class SpeedyUpdateRequestBuilder<T> {

    private final ObjectNode entity = JsonUtil.getObjectMapper().createObjectNode();
    @Getter
    private final String entityName;
    private final SpeedyApi<T> speedyApi;

    public SpeedyUpdateRequestBuilder(String entityName, SpeedyApi<T> speedyApi) {
        this.entityName = entityName;
        this.speedyApi = speedyApi;
    }

    /**
     * Add a key field (typically for identifying the record to update).
     *
     * @param field the field name
     * @param value the field value
     * @return this builder instance
     */
    public SpeedyUpdateRequestBuilder<T> key(String field, Object value) {
        entity.set(field, JsonUtil.getObjectMapper().convertValue(value, JsonNode.class));
        return this;
    }

    /**
     * Add a field to be updated.
     *
     * @param field the field name
     * @param value the field value
     * @return this builder instance
     */
    public SpeedyUpdateRequestBuilder<T> field(String field, Object value) {
        entity.set(field, JsonUtil.getObjectMapper().convertValue(value, JsonNode.class));
        return this;
    }


    /**
     * Build the SpeedyUpdateRequest without executing it.
     *
     * @return the constructed SpeedyUpdateRequest
     */
    public SpeedyUpdateRequest build() {
        SpeedyUpdateRequest request = new SpeedyUpdateRequest();
        request.setEntity(entityName);
        request.setBody(entity);
        return request;
    }

    /**
     * Build and execute the update request using the configured SpeedyApi.
     *
     * @return the response from the API
     */
    public T execute() throws Exception {
        SpeedyUpdateRequest request = build();
        return speedyApi.update(request);
    }
}
