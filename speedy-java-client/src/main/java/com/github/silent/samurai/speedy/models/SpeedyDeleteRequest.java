package com.github.silent.samurai.speedy.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyDeleteRequest {

    private String entity;
    private ArrayNode pkToDelete;

    public static SpeedyDeleteRequestBuilder builder(String entity) {
        return new SpeedyDeleteRequestBuilder(entity);
    }

}
