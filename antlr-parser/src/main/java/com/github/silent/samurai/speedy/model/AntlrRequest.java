package com.github.silent.samurai.speedy.model;

import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.LinkedList;
import java.util.List;

@Data
public class AntlrRequest {

    private String resource;
    private MultiValueMap<String, Filter> keywords = new LinkedMultiValueMap<>();
    private MultiValueMap<String, FilterValue> query = new LinkedMultiValueMap<>();
    private List<String> arguments = new LinkedList<>();
    private String fragment;

}
