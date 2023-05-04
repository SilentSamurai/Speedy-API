package com.github.silent.samurai.request.post;

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
import java.util.LinkedList;
import java.util.List;

@Data
public class PostRequestContext implements ResponseReturningRequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final MetaModelProcessor metaModelProcessor;
    private final EntityManager entityManager;
    private final ValidationProcessor validationProcessor;
    private final List<Object> parsedObjects = new LinkedList<>();

    private SpeedyUriContext parser;

    public PostRequestContext(HttpServletRequest request,
                              HttpServletResponse response,
                              MetaModelProcessor metaModelProcessor,
                              EntityManager entityManager,
                              ValidationProcessor validationProcessor) {
        this.request = request;
        this.response = response;
        this.metaModelProcessor = metaModelProcessor;
        this.validationProcessor = validationProcessor;
        this.entityManager = entityManager;
    }

    @Override
    public int getSerializationType() {
        return IResponseSerializer.MULTIPLE_ENTITY;
    }

    @Override
    public EntityMetadata getEntityMetadata() {
        return parser.getPrimaryResource().getResourceMetadata();
    }
}
