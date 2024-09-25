package com.github.silent.samurai.speedy.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


@Getter
@Setter
public class SpeedyUpdateRequest {

    private JsonNode body;
    private String entity;
    private ObjectNode pk;

    public static SpeedyUpdateRequestBuilder builder(String entity) {
        return new SpeedyUpdateRequestBuilder(entity);
    }

}
