package com.github.silent.samurai.speedy.api.client.models;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpeedyResponse {

    private Integer pageIndex;
    private Integer pageSize;
    private Integer totalPageCount;
    private JsonNode payload;

}
