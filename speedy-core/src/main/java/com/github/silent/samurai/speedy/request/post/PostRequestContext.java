package com.github.silent.samurai.speedy.request.post;

import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.VirtualEntityProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.ResponseReturningRequestContext;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class PostRequestContext implements ResponseReturningRequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final MetaModelProcessor metaModelProcessor;
    private final EntityManager entityManager;
    private final ValidationProcessor validationProcessor;
    private final EventProcessor eventProcessor;
    private final VirtualEntityProcessor vEntityProcessor;
    private final List<SpeedyEntity> parsedEntity = new LinkedList<>();

    EntityMetadata entityMetadata;

    public PostRequestContext(HttpServletRequest request,
                              HttpServletResponse response,
                              MetaModelProcessor metaModelProcessor,
                              EntityManager entityManager,
                              ValidationProcessor validationProcessor,
                              EventProcessor eventProcessor, VirtualEntityProcessor vEntityProcessor) {
        this.request = request;
        this.response = response;
        this.metaModelProcessor = metaModelProcessor;
        this.validationProcessor = validationProcessor;
        this.entityManager = entityManager;
        this.eventProcessor = eventProcessor;
        this.vEntityProcessor = vEntityProcessor;
    }

    @Override
    public int getSerializationType() {
        return IResponseSerializer.MULTIPLE_ENTITY;
    }

}
