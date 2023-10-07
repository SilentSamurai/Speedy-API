package com.github.silent.samurai.speedy.request.put;

import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.ResponseReturningRequestContext;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
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
    private final EventProcessor eventProcessor;

    private SpeedyUriContext parser;
    private Object entityInstance;

    public PutRequestContext(HttpServletRequest request,
                             HttpServletResponse response,
                             MetaModelProcessor metaModelProcessor,
                             EntityManager entityManager,
                             ValidationProcessor validationProcessor, EventProcessor eventProcessor) {
        this.request = request;
        this.response = response;
        this.metaModelProcessor = metaModelProcessor;
        this.entityManager = entityManager;
        this.validationProcessor = validationProcessor;
        this.eventProcessor = eventProcessor;
    }

    @Override
    public int getSerializationType() {
        return IResponseSerializer.SINGLE_ENTITY;
    }

    public EntityMetadata getEntityMetadata() {
        return parser.getPrimaryResource().getResourceMetadata();
    }
}
