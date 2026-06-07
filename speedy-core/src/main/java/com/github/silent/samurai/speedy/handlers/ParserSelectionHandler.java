package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.request.SpeedyRequest;
import com.github.silent.samurai.speedy.serializers.JSONBodyParser;
import lombok.extern.slf4j.Slf4j;

/// Selects the appropriate IRequestBodyParser based on the Content-Type header.
///
/// Mirrors SerializerSelectionHandler on the response side. Reads the
/// Content-Type header from the request and instantiates the matching parser.
/// Currently defaults to JSONBodyParser for application/json, */*, or null.
/// Future YAML/XML parsers are selected by their respective MIME types.
///
/// @see SerializerSelectionHandler
/// @see IRequestBodyParser
@Slf4j
public class ParserSelectionHandler implements Handler {

    final Handler next;

    public ParserSelectionHandler(Handler next) {
        this.next = next;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        SpeedyRequest request = context.getRequest();
        String contentType = request.getHeaders().get("Content-Type");

        IRequestBodyParser parser;

        if (contentType == null || contentType.contains("application/json")) {
            parser = new JSONBodyParser();
        } else {
            log.warn("Unsupported Content-Type '{}', defaulting to JSON", contentType);
            parser = new JSONBodyParser();
        }

        context.setRequestBodyParser(parser);
        next.process(context);
    }
}
