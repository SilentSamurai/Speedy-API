package com.github.silent.samurai.speedy.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.silent.samurai.speedy.utils.CommonUtil;

public class SpeedyEntityBuilder {

    ObjectNode entity = CommonUtil.json().createObjectNode();


    public SpeedyEntityBuilder addField(String field, Object value) {
        entity.set(field, CommonUtil.json().convertValue(value, JsonNode.class));
        return this;
    }

    public ObjectNode build() {
        return entity;
    }

    public ArrayNode wrapInArray() {
        ArrayNode arrayNode = CommonUtil.json().createArrayNode();
        arrayNode.add(build());
        return arrayNode;
    }

    public static SpeedyEntityBuilder builder() {
        return new SpeedyEntityBuilder();
    }


}
