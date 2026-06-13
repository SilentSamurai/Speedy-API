package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.conversion.registry.JsonRegistry;
import com.github.silent.samurai.speedy.http.request.JSONBodyParser;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.request.RequestContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParserSelectionHandler implements Handler {

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        SpeedyHeaders headers = context.get(SpeedyHeaders.class);
        String contentType = headers.get("Content-Type");
        /// Extract the JSON registry from the conversion context and pass it to
        /// {@link JSONBodyParser} so that request-body JSON can be decoded type-safely.
        JsonRegistry jr = context.get(ConversionContext.class).get(JsonRegistry.class);

        IRequestBodyParser parser;

        if (contentType == null || contentType.contains("application/json")) {
            parser = new JSONBodyParser(jr);
        } else {
            log.warn("Unsupported Content-Type '{}', defaulting to JSON", contentType);
            parser = new JSONBodyParser(jr);
        }

        context.put(IRequestBodyParser.class, parser);
    }
}
