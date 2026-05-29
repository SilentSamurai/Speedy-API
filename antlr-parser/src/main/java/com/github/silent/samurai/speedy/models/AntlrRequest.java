package com.github.silent.samurai.speedy.models;

import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.LinkedList;
import java.util.List;


@Data
public class AntlrRequest {

    private final MultiValueMap<String, UrlQuery> queries = new LinkedMultiValueMap<>();
    private List<ResourceRequest> requestList = new LinkedList<>();
    private String fragment;


}
