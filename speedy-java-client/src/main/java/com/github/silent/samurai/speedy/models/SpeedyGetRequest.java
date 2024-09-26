package com.github.silent.samurai.speedy.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyGetRequest {

    private String entity;
    private ObjectNode pk;

    public static SpeedyGetRequestBuilder builder(String entity) {
        return new SpeedyGetRequestBuilder(entity);
    }

}
