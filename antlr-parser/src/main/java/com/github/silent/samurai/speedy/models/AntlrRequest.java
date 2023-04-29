package com.github.silent.samurai.speedy.models;

import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class AntlrRequest {

    private final List<String> filterOrder = new LinkedList<>();
    private final MultiValueMap<String, Query> queries = new LinkedMultiValueMap<>();
    private final Map<String, Filter> filters = new HashMap<>();
    private String resource;
    private List<String> arguments = new LinkedList<>();
    private String fragment;

    public void addFilter(Filter filter) {
        String id = filter.getField() + "_" + filters.size();
        filter.setInternalId(id);
        filters.put(id, filter);
        filterOrder.add(id);
    }

}
