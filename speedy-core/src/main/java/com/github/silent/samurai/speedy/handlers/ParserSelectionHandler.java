package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.engine.ContentNegotiationManager;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.ISpeedyIoProvider;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.serialization.WalkingRequestParser;

public class ParserSelectionHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    private final ContentNegotiationManager manager;

    public ParserSelectionHandler(ContentNegotiationManager manager) {
        this.manager = manager;
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyHeaders headers = context.get(SpeedyHeaders.class);
        String contentType = headers.get("Content-Type");
        ConversionContext conversionContext = context.get(ConversionContext.class);

        ISpeedyIoProvider selected = manager.selectProvider(contentType);
        context.put(IRequestBodyParser.class,
                new WalkingRequestParser(selected.getContentType(), selected.createReader(conversionContext)));
    }
}
