package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.request.RequestContext;
import jakarta.servlet.http.HttpServletResponse;

public class SpeedyResponseWriterHandler implements Handler {

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        if (context.has(IResponseSerializerV2.class) && context.has(SpeedyResponse.class)) {
            context.get(IResponseSerializerV2.class).write(
                    context.get(SpeedyResponse.class),
                    context.get(HttpServletResponse.class)
            );
        } else {
            throw new InternalServerError("Failed to generate Response");
        }
    }
}
