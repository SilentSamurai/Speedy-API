package com.github.silent.samurai.speedy.request.delete;

import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.VirtualEntityProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.ResponseReturningRequestContext;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import lombok.Data;

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
    private final QueryProcessor queryProcessor;
    private final EventProcessor eventProcessor;
    private final VirtualEntityProcessor vEntityProcessor;
    private final List<SpeedyEntityKey> keysToBeRemoved = new LinkedList<>();

    private EntityMetadata entityMetadata;

    public DeleteRequestContext(HttpServletRequest request,
                                HttpServletResponse response,
                                MetaModelProcessor metaModelProcessor,
                                ValidationProcessor validationProcessor,
                                QueryProcessor queryProcessor,
                                EventProcessor eventProcessor,
                                VirtualEntityProcessor vEntityProcessor) {
        this.request = request;
        this.response = response;
        this.metaModelProcessor = metaModelProcessor;
        this.validationProcessor = validationProcessor;
        this.queryProcessor = queryProcessor;
        this.eventProcessor = eventProcessor;
        this.vEntityProcessor = vEntityProcessor;
    }

    @Override
    public int getSerializationType() {
        return IResponseSerializer.MULTIPLE_ENTITY;
    }
}
