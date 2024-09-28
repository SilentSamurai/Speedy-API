package com.github.silent.samurai.speedy.api.client.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.api.client.models.SpeedyDeleteRequest;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.Getter;

public class SpeedyDeleteRequestBuilder {

    ObjectNode pk = CommonUtil.json().createObjectNode();

    @Getter
    private final String entityName;

    public SpeedyDeleteRequestBuilder(String entityName) {
        this.entityName = entityName;
    }

    public SpeedyDeleteRequestBuilder key(String field, Object value) {
        pk.set(field, CommonUtil.json().convertValue(value, JsonNode.class));
        return this;
    }

    public SpeedyDeleteRequest build() {
        SpeedyDeleteRequest request = new SpeedyDeleteRequest();
        request.setEntity(entityName);
        ArrayNode arrayNode = CommonUtil.json().createArrayNode();
        arrayNode.add(pk);
        request.setPkToDelete(arrayNode);
        return request;
    }

}
