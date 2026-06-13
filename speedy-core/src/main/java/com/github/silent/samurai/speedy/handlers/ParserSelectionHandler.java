package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParserProvider;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.request.RequestContext;

public class ParserSelectionHandler implements Handler {

    private final ContentNegotiationManager manager;

    public ParserSelectionHandler(ContentNegotiationManager manager) {
        this.manager = manager;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        SpeedyHeaders headers = context.get(SpeedyHeaders.class);
        String contentType = headers.get("Content-Type");
        ConversionContext conversionContext = context.get(ConversionContext.class);

        IRequestBodyParserProvider selected = manager.selectParser(contentType);
        context.put(IRequestBodyParser.class, selected.create(conversionContext));
    }
}
