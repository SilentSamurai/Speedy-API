package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.engine.ContentNegotiationManager;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyIoProvider;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.request.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

public class SerializerSelectionHandler implements Handler {

    private final ContentNegotiationManager manager;

    public SerializerSelectionHandler(ContentNegotiationManager manager) {
        this.manager = manager;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        HttpServletRequest request = context.get(HttpServletRequest.class);
        String accept = request.getHeader("Accept");
        ConversionContext conversionContext = context.get(ConversionContext.class);

        ISpeedyIoProvider selected = manager.selectProvider(accept);
        context.put(IResponseSerializerV2.class,
                selected.createSerializer(context.get(MetaModel.class), conversionContext));
    }
}
