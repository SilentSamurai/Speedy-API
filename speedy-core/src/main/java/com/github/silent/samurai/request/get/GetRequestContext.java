package com.github.silent.samurai.request.get;

import com.github.silent.samurai.Request;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.ResponseReturningRequestContext;
import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class GetRequestContext implements ResponseReturningRequestContext {

    private HttpServletRequest httpServletRequest;
    private MetaModelProcessor metaModelProcessor;
    private int serializationType;
    private EntityManager entityManager;

    private Request request;
    private EntityMetadata resourceMetadata;
    private boolean primaryKey = false;
    private Map<String, String> keywords = new HashMap<>();
    private List<String> arguments = new LinkedList<>();
    private String secondaryResource;

    private MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

    public GetRequestContext(HttpServletRequest httpServletRequest, MetaModelProcessor metaModelProcessor, EntityManager entityManager) {
        this.httpServletRequest = httpServletRequest;
        this.metaModelProcessor = metaModelProcessor;
        this.entityManager = entityManager;
    }
}
