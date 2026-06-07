package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;

public class SpeedyResponseWriterHandler implements Handler {

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        if (context.getResponseSerializer() != null && context.getSpeedyResponse() != null) {
            context.getResponseSerializer().write(
                    context.getSpeedyResponse(),
                    context.getHttpServletResponse()
            );
        } else {
            throw new InternalServerError("Failed to generate Response");
        }
    }
}
