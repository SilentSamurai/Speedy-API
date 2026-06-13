package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.conversion.registry.JsonRegistry;
import com.github.silent.samurai.speedy.http.response.JSONSerializerV2;
import com.github.silent.samurai.speedy.request.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializerSelectionHandler implements Handler {

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        HttpServletRequest request = context.getHttpServletRequest();
        String accept = request.getHeader("Accept");
        /// Extract the JSON registry from the conversion context and pass it to
        /// {@link JSONSerializerV2} so that response SpeedyValues can be encoded to JSON.
        JsonRegistry jr = context.getConversionContext().get(JsonRegistry.class);

        if (accept == null || accept.contains("*/*") || accept.contains("application/json")) {
            context.setResponseSerializer(
                    new JSONSerializerV2(context.getMetaModel(),
                            context.getEntityMetadata(),
                            jr)
            );
        } else {
            log.warn("Unsupported Accept header '{}', defaulting to JSON", accept);
            context.setResponseSerializer(
                    new JSONSerializerV2(context.getMetaModel(),
                            context.getEntityMetadata(),
                            jr)
            );
        }
    }
}
