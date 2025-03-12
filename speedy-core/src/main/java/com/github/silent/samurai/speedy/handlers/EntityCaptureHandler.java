package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

public class EntityCaptureHandler implements Handler {

    final Handler next;

    public EntityCaptureHandler(Handler next) {
        this.next = next;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        MetaModel metaModel = context.getMetaModel();
        String requestURI = context.getRequestUri();

        SpeedyUriContext parser = new SpeedyUriContext(metaModel, requestURI);
        SpeedyQuery uriSpeedyQuery = parser.parse();

        EntityMetadata resourceMetadata = uriSpeedyQuery.getFrom();
        context.setEntityMetadata(resourceMetadata);

        next.process(context);
    }
}
