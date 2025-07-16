package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.request.IResponseContext;
import com.github.silent.samurai.speedy.request.RequestContext;

public class SpeedyResponseWriterHandler implements Handler {

    final Handler next;

    public SpeedyResponseWriterHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        if (context.getResponseSerializer() != null) {
            IResponseSerializerV2 responseSerializer = context.getResponseSerializer();

            IResponseContext responseContext = IResponseContext.builder()
                    .request(context.getHttpServletRequest())
                    .response(context.getHttpServletResponse())
                    .metaModelProcessor(context.getMetaModel())
                    .entityMetadata(context.getEntityMetadata())
                    .build();

            responseSerializer.write(responseContext);

        } else {
            throw new InternalServerError("Failed to generate Response");
        }
        next.process(context);
    }
}
