package com.github.silent.samurai.speedy.request;

import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.VirtualEntityProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.IRequestContext;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import lombok.Getter;
import lombok.Setter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Getter
@Setter
public class IRequestContextImpl implements IRequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final MetaModelProcessor metaModelProcessor;
    private final ValidationProcessor validationProcessor;
    private final EventProcessor eventProcessor;
    private final VirtualEntityProcessor vEntityProcessor;
    private final QueryProcessor queryProcessor;
    private final EntityMetadata entityMetadata;

    public IRequestContextImpl(HttpServletRequest request,
                               HttpServletResponse response,
                               MetaModelProcessor metaModelProcessor,
                               ValidationProcessor validationProcessor,
                               EventProcessor eventProcessor,
                               VirtualEntityProcessor vEntityProcessor,
                               QueryProcessor queryProcessor,
                               EntityMetadata metadata) {
        this.request = request;
        this.response = response;
        this.metaModelProcessor = metaModelProcessor;
        this.validationProcessor = validationProcessor;
        this.eventProcessor = eventProcessor;
        this.vEntityProcessor = vEntityProcessor;
        this.queryProcessor = queryProcessor;
        this.entityMetadata = metadata;
    }


    public IResponseContext.ResponseContextBuilder createResponseContext() {
        return IResponseContext.builder()
                .request(request)
                .response(response)
                .metaModelProcessor(metaModelProcessor)
                .entityMetadata(entityMetadata);
    }

}
