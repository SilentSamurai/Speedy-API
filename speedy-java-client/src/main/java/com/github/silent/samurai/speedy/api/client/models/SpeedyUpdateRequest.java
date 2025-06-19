package com.github.silent.samurai.speedy.api.client.models;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyUpdateRequest {

    private JsonNode body;
    private String entity;

}
