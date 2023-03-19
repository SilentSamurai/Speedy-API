package com.github.silent.samurai.request.get;

import com.github.silent.samurai.Request;
import com.github.silent.samurai.interfaces.EntityMetadata;
import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class GETRequestContext {

    private Request request;
    private EntityMetadata resourceMetadata;
    private boolean primaryKey = false;
    private Map<String, String> keywords = new HashMap<>();
    private List<String> arguments = new LinkedList<>();
    public String secondaryResource;
    private int serializationType;
    private MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
}
