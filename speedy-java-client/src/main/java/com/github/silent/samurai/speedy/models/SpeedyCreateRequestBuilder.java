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
        entity.set(field, CommonUtil.json().convertValue(value, JsonNode.class));
        return this;
    }

    public SpeedyCreateRequest build() {
        SpeedyCreateRequest speedyCreateRequest = new SpeedyCreateRequest();
        speedyCreateRequest.setEntity(entityName);
        speedyCreateRequest.setBody(entity);
        return speedyCreateRequest;
    }

    public SpeedyCreateRequest wrapInArray() {
        ArrayNode arrayNode = CommonUtil.json().createArrayNode();
        arrayNode.add(entity);
        SpeedyCreateRequest speedyCreateRequest = new SpeedyCreateRequest();
        speedyCreateRequest.setEntity(entityName);
        speedyCreateRequest.setBody(arrayNode);
        return speedyCreateRequest;
    }

    public static SpeedyCreateRequestBuilder builder(String entityName) {
        return new SpeedyCreateRequestBuilder(entityName);
    }


}
