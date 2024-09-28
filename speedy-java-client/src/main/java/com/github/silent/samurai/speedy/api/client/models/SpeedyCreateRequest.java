package com.github.silent.samurai.speedy.api.client.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.api.client.builder.SpeedyCreateRequestBuilder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyCreateRequest {

    private JsonNode body;
    private String entity;

    public SpeedyCreateRequest() {
    }

    public static SpeedyCreateRequestBuilder builder(String entityName) {
        return new SpeedyCreateRequestBuilder(entityName);
    }

}
