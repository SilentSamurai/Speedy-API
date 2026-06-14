package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.engine.ContentNegotiationManager;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyIoProvider;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.context.SpeedyContext;

public class SerializerSelectionHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    private final ContentNegotiationManager manager;

    public SerializerSelectionHandler(ContentNegotiationManager manager) {
        this.manager = manager;
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyHeaders headers = context.get(SpeedyHeaders.class);
        String accept = headers.get("Accept");
        ConversionContext conversionContext = context.get(ConversionContext.class);

        ISpeedyIoProvider selected = manager.selectProvider(accept);
        context.put(IResponseSerializerV2.class,
                selected.createSerializer(context.get(MetaModel.class), conversionContext));
    }
}
