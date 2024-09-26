package com.github.silent.samurai.speedy.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.Getter;

public class SpeedyGetRequestBuilder {

    ObjectNode entity = CommonUtil.json().createObjectNode();
    ObjectNode pk = CommonUtil.json().createObjectNode();

    @Getter
    private final String entityName;

    public SpeedyGetRequestBuilder(String entityName) {
        this.entityName = entityName;
    }

    public SpeedyGetRequestBuilder key(String field, Object value) {
        pk.set(field, CommonUtil.json().convertValue(value, JsonNode.class));
        return this;
    }

    public SpeedyGetRequest build() {
        SpeedyGetRequest request = new SpeedyGetRequest();
        request.setEntity(entityName);
        request.setPk(pk);
        return request;
    }

    public static SpeedyGetRequestBuilder builder(String entityName) {
        return new SpeedyGetRequestBuilder(entityName);
    }


}
