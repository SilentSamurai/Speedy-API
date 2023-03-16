package com.github.silent.samurai.metamodel;

import lombok.Data;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

@Data
public class RequestInfo {

    public String resourceType;
    public boolean primaryKey;
    public Map<String, String> filters;
    public String secondaryResourceType;
    public int serializationType;
    public MultiValueMap<String, String> queryParams;
}
