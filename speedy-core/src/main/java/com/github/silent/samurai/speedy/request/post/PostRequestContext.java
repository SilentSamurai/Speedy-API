package com.github.silent.samurai.speedy.request.post;

import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.VirtualEntityProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.ResponseReturningRequestContext;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import lombok.Getter;
import lombok.Setter;

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
    private final ValidationProcessor validationProcessor;
    private final EventProcessor eventProcessor;
    private final VirtualEntityProcessor vEntityProcessor;
    private final List<SpeedyEntity> parsedEntity = new LinkedList<>();
    private final QueryProcessor queryProcessor;

    SpeedyQuery speedyQuery;

    public PostRequestContext(HttpServletRequest request,
                              HttpServletResponse response,
                              MetaModelProcessor metaModelProcessor,
                              ValidationProcessor validationProcessor,
                              EventProcessor eventProcessor,
                              VirtualEntityProcessor vEntityProcessor,
                              QueryProcessor queryProcessor) {
        this.request = request;
        this.response = response;
        this.metaModelProcessor = metaModelProcessor;
        this.validationProcessor = validationProcessor;
        this.eventProcessor = eventProcessor;
        this.vEntityProcessor = vEntityProcessor;
        this.queryProcessor = queryProcessor;
    }

    @Override
    public int getSerializationType() {
        return IResponseSerializer.MULTIPLE_ENTITY;
    }

    @Override
    public SpeedyQuery getQuery() {
        return speedyQuery;
    }

    @Override
    public EntityMetadata getEntityMetadata() {
        return speedyQuery.getFrom();
    }
}
