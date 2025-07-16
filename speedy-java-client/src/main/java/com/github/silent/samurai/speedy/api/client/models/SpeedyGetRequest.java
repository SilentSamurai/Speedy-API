package com.github.silent.samurai.speedy.api.client.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SpeedyGetRequest {

    private String entity;
    private ObjectNode pk;

}
