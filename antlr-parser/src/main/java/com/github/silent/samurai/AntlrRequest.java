package com.github.silent.samurai;

import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class AntlrRequest {

    private String resource;
    private Map<String, String> keywords = new HashMap<>();
    private MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    private List<String> arguments = new LinkedList<>();
    private String fragment;

}
