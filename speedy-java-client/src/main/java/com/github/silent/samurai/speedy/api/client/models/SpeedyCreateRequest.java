package com.github.silent.samurai.speedy.api.client.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyCreateRequest {

    private JsonNode body;
    private String entity;

    public static SpeedyQuery builder() {
        return null;
    }
}
