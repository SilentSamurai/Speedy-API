package com.github.silent.samurai.speedy.request.get;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.ResponseReturningRequestContext;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import lombok.Data;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Data
public class GetRequestContext implements ResponseReturningRequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final MetaModelProcessor metaModelProcessor;
    private final EntityManager entityManager;

    private SpeedyUriContext parser;

    public GetRequestContext(HttpServletRequest request, HttpServletResponse response, MetaModelProcessor metaModelProcessor, EntityManager entityManager) {
        this.request = request;
        this.response = response;
        this.metaModelProcessor = metaModelProcessor;
        this.entityManager = entityManager;
    }

    public EntityMetadata getEntityMetadata() {
        return parser.getPrimaryResource().getResourceMetadata();
    }

    @Override
    public int getSerializationType() {
        if (parser.getPrimaryResource().isOnlyIdentifiersPresent()) {
            return IResponseSerializer.SINGLE_ENTITY;
        }
        return IResponseSerializer.MULTIPLE_ENTITY;
    }
}
