package com.github.silent.samurai.metamodel;

import java.util.Map;

public class RequestInfo {

    public String resourceType;
    public boolean primaryKey;
    public Map<String, String> filters;
    public String secondaryResourceType;
    public int serializationType;
}
