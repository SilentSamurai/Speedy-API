package com.github.silent.samurai.request.put;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.IResponseSerializer;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.ResponseReturningRequestContext;
import com.github.silent.samurai.parser.SpeedyUriContext;
import com.github.silent.samurai.validation.ValidationProcessor;
import lombok.Data;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Data
public class PutRequestContext implements ResponseReturningRequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final MetaModelProcessor metaModelProcessor;
    private final EntityManager entityManager;
    private final ValidationProcessor validationProcessor;

    private SpeedyUriContext parser;
    private Object entityInstance;

    public PutRequestContext(HttpServletRequest request,
                             HttpServletResponse response,
                             MetaModelProcessor metaModelProcessor,
                             EntityManager entityManager,
                             ValidationProcessor validationProcessor) {
        this.request = request;
        this.response = response;
        this.metaModelProcessor = metaModelProcessor;
        this.entityManager = entityManager;
        this.validationProcessor = validationProcessor;
    }

    @Override
    public int getSerializationType() {
        return IResponseSerializer.SINGLE_ENTITY;
    }

    public EntityMetadata getEntityMetadata() {
        return parser.getPrimaryResource().getResourceMetadata();
    }
}
