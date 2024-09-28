package com.github.silent.samurai.speedy.api.client.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.api.client.builder.SpeedyGetRequestBuilder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyGetRequest {

    private String entity;
    private ObjectNode pk;

    public SpeedyGetRequest() {
    }

    public static SpeedyGetRequestBuilder builder(String entity) {
        return new SpeedyGetRequestBuilder(entity);
    }

}
