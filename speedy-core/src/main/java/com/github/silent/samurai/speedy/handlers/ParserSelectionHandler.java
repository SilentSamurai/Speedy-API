package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.engine.ContentNegotiationManager;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.request.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.request.ISpeedyIoProvider;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.serialization.DefaultRequestParser;

public class ParserSelectionHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    private final ContentNegotiationManager manager;

    public ParserSelectionHandler(ContentNegotiationManager manager) {
        this.manager = manager;
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyHeaders headers = context.get(SpeedyHeaders.class);
        String contentType = headers.get("Content-Type");

        ISpeedyIoProvider selected = manager.selectProvider(contentType);
        context.put(IRequestBodyParser.class,
                new DefaultRequestParser(selected.getContentType(), selected.createReader()));
    }
}
