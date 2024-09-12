package com.github.silent.samurai.speedy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SpdyQ {

    public static SpdyQBuilder builder() {
        return new SpdyQBuilder();
    }

    public static JsonNode whereEq(String key, Object value) throws JsonProcessingException {
        return builder()
                .$whereCondition(key, SpdyQBuilder.$eq(value))
                .build();
    }

    public static JsonNode where(String key, ObjectNode value) {
        return builder().$whereCondition(key, value).build();
    }

}
