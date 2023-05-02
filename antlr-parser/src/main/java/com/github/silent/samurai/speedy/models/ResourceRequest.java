package com.github.silent.samurai.speedy.models;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ResourceRequest {

    private final List<String> filterOrder = new LinkedList<>();
    private final Map<String, Filter> filters = new HashMap<>();
    private String resource;
    private List<String> arguments = new LinkedList<>();

    public void addFilter(Filter filter) {
        String id = filter.getField() + "_" + filters.size();
        filter.setInternalId(id);
        filters.put(id, filter);
        filterOrder.add(id);
    }

}
