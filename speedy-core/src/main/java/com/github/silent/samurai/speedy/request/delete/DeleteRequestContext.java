package com.github.silent.samurai.speedy.request.delete;

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
import java.util.LinkedList;
import java.util.List;

@Data
public class DeleteRequestContext implements ResponseReturningRequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final MetaModelProcessor metaModelProcessor;
    private final ValidationProcessor validationProcessor;
    private final EntityManager entityManager;
    private final List<Object> objectsToBeRemoved = new LinkedList<>();

    private SpeedyUriContext parser;

    public DeleteRequestContext(HttpServletRequest request,
                                HttpServletResponse response, MetaModelProcessor metaModelProcessor,
                                ValidationProcessor validationProcessor,
                                EntityManager entityManager) {
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
