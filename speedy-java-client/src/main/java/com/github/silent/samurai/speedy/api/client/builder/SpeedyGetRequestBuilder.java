package com.github.silent.samurai.speedy.api.client.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.api.client.SpeedyApi;
import com.github.silent.samurai.speedy.api.client.models.SpeedyGetRequest;
import com.github.silent.samurai.speedy.api.client.utils.JsonUtil;
import lombok.Getter;

public class SpeedyGetRequestBuilder<T> {

    private final ObjectNode pk = JsonUtil.getObjectMapper().createObjectNode();
    @Getter
    private final String entityName;
    private final SpeedyApi<T> speedyApi;

    public SpeedyGetRequestBuilder(String entityName, SpeedyApi<T> speedyApi) {
        this.entityName = entityName;
        this.speedyApi = speedyApi;
    }

    /**
     * @param field the field name
     * @param value the field value
     * @return this builder instance
     */
    public SpeedyGetRequestBuilder<T> key(String field, Object value) {
        pk.set(field, JsonUtil.getObjectMapper().convertValue(value, JsonNode.class));
        return this;
    }

    /**
     * Build the SpeedyGetRequest without executing it.
     *
     * @return the constructed SpeedyGetRequest
     */
    private SpeedyGetRequest build() {
        SpeedyGetRequest request = new SpeedyGetRequest();
        request.setEntity(entityName);
        request.setPk(pk);
        return request;
    }

    /**
     * Build and execute the get request using the configured SpeedyApi.
     *
     * @return the response from the API
     */
    public T execute() throws Exception {
        SpeedyGetRequest request = build();
        return speedyApi.get(request);
    }
}
