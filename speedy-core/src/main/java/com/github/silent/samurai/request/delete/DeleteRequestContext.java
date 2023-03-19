package com.github.silent.samurai.request.delete;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.RequestContext;
import lombok.Data;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;

@Data
public class DeleteRequestContext implements RequestContext {

    private final HttpServletRequest httpServletRequest;
    private final MetaModelProcessor metaModelProcessor;
    private EntityManager entityManager;

    private EntityMetadata entityMetadata;
    private String resource;
    private List<Object> parsedObjects = new LinkedList<>();

    public DeleteRequestContext(HttpServletRequest httpServletRequest, MetaModelProcessor metaModelProcessor, EntityManager entityManager) {
        this.httpServletRequest = httpServletRequest;
        this.metaModelProcessor = metaModelProcessor;
        this.entityManager = entityManager;
    }
}
