package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializerSelectionHandler implements Handler {

    private static final Logger log = LoggerFactory.getLogger(SerializerSelectionHandler.class);

    final Handler next;

    public SerializerSelectionHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        HttpServletRequest request = context.getHttpServletRequest();
        String accept = request.getHeader("Accept");

        if (accept == null || accept.contains("*/*") || accept.contains("application/json")) {
            context.setResponseSerializer(
                    new JSONSerializerV2(context.getMetaModel(), context.getEntityMetadata())
            );
        } else {
            log.warn("Unsupported Accept header '{}', defaulting to JSON", accept);
            context.setResponseSerializer(
                    new JSONSerializerV2(context.getMetaModel(), context.getEntityMetadata())
            );
        }

        next.process(context);
    }
}
