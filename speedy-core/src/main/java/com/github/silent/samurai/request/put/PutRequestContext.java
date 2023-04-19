package com.github.silent.samurai.request.put;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.RequestContext;
import com.github.silent.samurai.validation.ValidationProcessor;
import lombok.Data;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

@Data
public class PutRequestContext implements RequestContext {

    private final HttpServletRequest httpServletRequest;
    private final MetaModelProcessor metaModelProcessor;
    private EntityManager entityManager;

    private EntityMetadata entityMetadata;
    private final ValidationProcessor validationProcessor;
    private String resource;
    private Object entityInstance;

    public PutRequestContext(HttpServletRequest httpServletRequest,
                             MetaModelProcessor metaModelProcessor,
                             EntityManager entityManager,
                             ValidationProcessor validationProcessor) {
        this.httpServletRequest = httpServletRequest;
        this.metaModelProcessor = metaModelProcessor;
        this.entityManager = entityManager;
        this.validationProcessor = validationProcessor;
    }

}
