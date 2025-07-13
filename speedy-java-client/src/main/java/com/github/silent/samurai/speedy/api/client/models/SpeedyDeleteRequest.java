package com.github.silent.samurai.speedy.api.client.models;

import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyDeleteRequest {

    private String entity;
    private ArrayNode pkToDelete;

}
