package com.github.silent.samurai.speedy.api.client.models;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.silent.samurai.speedy.api.client.builder.SpeedyDeleteRequestBuilder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyDeleteRequest {

    private String entity;
    private ArrayNode pkToDelete;

    public SpeedyDeleteRequest() {
    }

    public static SpeedyDeleteRequestBuilder builder(String entity) {
        return new SpeedyDeleteRequestBuilder(entity);
    }

}
