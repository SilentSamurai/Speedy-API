package com.github.silent.samurai.speedy.models;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyCreateRequest {

    private JsonNode body;
    private String entity;

    public static SpeedyCreateRequestBuilder builder(String entityName) {
        return new SpeedyCreateRequestBuilder(entityName);
    }

}
