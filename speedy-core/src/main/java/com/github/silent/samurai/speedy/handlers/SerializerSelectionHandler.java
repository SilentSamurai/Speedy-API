package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.conversion.registry.JsonRegistry;
import com.github.silent.samurai.speedy.http.response.JSONSerializerV2;
import com.github.silent.samurai.speedy.request.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializerSelectionHandler implements Handler {

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        HttpServletRequest request = context.get(HttpServletRequest.class);
        String accept = request.getHeader("Accept");
        /// Extract the JSON registry from the conversion context and pass it to
        /// {@link JSONSerializerV2} so that response SpeedyValues can be encoded to JSON.
        JsonRegistry jr = context.get(ConversionContext.class).get(JsonRegistry.class);

        if (accept == null || accept.contains("*/*") || accept.contains("application/json")) {
            context.put(IResponseSerializerV2.class,
                    new JSONSerializerV2(context.get(MetaModel.class),
                            context.getEntityMetadata(),
                            jr)
            );
        } else {
            log.warn("Unsupported Accept header '{}', defaulting to JSON", accept);
            context.put(IResponseSerializerV2.class,
                    new JSONSerializerV2(context.get(MetaModel.class),
                            context.getEntityMetadata(),
                            jr)
            );
        }
    }
}
