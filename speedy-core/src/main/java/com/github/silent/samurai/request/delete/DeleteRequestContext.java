package com.github.silent.samurai.request.delete;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.RequestContext;
import com.github.silent.samurai.validation.ValidationProcessor;
import lombok.Data;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;

@Data
public class DeleteRequestContext implements RequestContext {

    private final HttpServletRequest request;
    private final MetaModelProcessor metaModelProcessor;
    private final ValidationProcessor validationProcessor;
    private EntityManager entityManager;

    private EntityMetadata entityMetadata;
    private String resource;
    private List<Object> objectsToBeRemoved = new LinkedList<>();

    public DeleteRequestContext(HttpServletRequest request,
                                MetaModelProcessor metaModelProcessor,
                                ValidationProcessor validationProcessor,
                                EntityManager entityManager) {
        this.request = request;
        this.metaModelProcessor = metaModelProcessor;
        this.validationProcessor = validationProcessor;
        this.entityManager = entityManager;
    }
}
