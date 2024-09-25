package com.github.silent.samurai.speedy.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.Getter;

public class SpeedyUpdateRequestBuilder {

    ObjectNode entity = CommonUtil.json().createObjectNode();
    ObjectNode pk = CommonUtil.json().createObjectNode();

    @Getter
    private final String entityName;

    public SpeedyUpdateRequestBuilder(String entityName) {
        this.entityName = entityName;
    }

    public SpeedyUpdateRequestBuilder key(String field, Object value) {
        pk.set(field, CommonUtil.json().convertValue(value, JsonNode.class));
        return this;
    }

    public SpeedyUpdateRequestBuilder field(String field, Object value) {
        entity.set(field, CommonUtil.json().convertValue(value, JsonNode.class));
        return this;
    }

    public SpeedyUpdateRequest build() {
        SpeedyUpdateRequest request = new SpeedyUpdateRequest();
        request.setEntity(entityName);
        request.setBody(entity);
        request.setPk(pk);
        return request;
    }

    public static SpeedyUpdateRequestBuilder builder(String entityName) {
        return new SpeedyUpdateRequestBuilder(entityName);
    }


}
