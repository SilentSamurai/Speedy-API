package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/// Selects the response serializer based on the Accept request header.
///
/// Currently only supports JSON via JSONSerializerV2. Future XML/YAML
/// serializer selection is handled by matching the Accept header to the
/// serializer's content type.
///
/// @see ParserSelectionHandler
/// @see IResponseSerializerV2
@Slf4j
public class SerializerSelectionHandler implements Handler {

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
                    new JSONSerializerV2(context.getMetaModel(),
                            context.getEntityMetadata())
            );
        } else {
            log.warn("Unsupported Accept header '{}', defaulting to JSON", accept);
            context.setResponseSerializer(
                    new JSONSerializerV2(context.getMetaModel(),
                            context.getEntityMetadata())
            );
        }

        next.process(context);
    }
}
