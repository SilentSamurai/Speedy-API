package com.github.silent.samurai.speedy.api.client.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.api.client.builder.SpeedyUpdateRequestBuilder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyUpdateRequest {

    private JsonNode body;
    private String entity;
    private ObjectNode pk;

    public SpeedyUpdateRequest() {
    }

    public static SpeedyUpdateRequestBuilder builder(String entity) {
        return new SpeedyUpdateRequestBuilder(entity);
    }

}
