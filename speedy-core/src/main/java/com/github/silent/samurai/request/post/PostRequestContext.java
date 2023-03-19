package com.github.silent.samurai.request.post;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.RequestContext;
import lombok.Data;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.LinkedList;
import java.util.List;

@Data
public class PostRequestContext implements RequestContext {

    private final HttpServletRequest httpServletRequest;
    private final MetaModelProcessor metaModelProcessor;
    private EntityManager entityManager;

    private EntityMetadata entityMetadata;
    private final Validator validator;
    private String resource;
    private List<Object> parsedObjects = new LinkedList<>();

    public PostRequestContext(HttpServletRequest httpServletRequest, MetaModelProcessor metaModelProcessor, EntityManager entityManager) {
        this.httpServletRequest = httpServletRequest;
        this.metaModelProcessor = metaModelProcessor;
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        this.entityManager = entityManager;
    }
}
