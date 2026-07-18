package com.github.silent.samurai.speedy.models;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@Data
public class AntlrRequest {

    private final Map<String, List<UrlQuery>> queries = new LinkedHashMap<>();
    private List<ResourceRequest> requestList = new LinkedList<>();
    private String fragment;


}
