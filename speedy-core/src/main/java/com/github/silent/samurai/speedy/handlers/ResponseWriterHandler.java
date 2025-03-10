package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.RequestContext;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.request.IResponseContext;
import com.github.silent.samurai.speedy.serializers.JSONSerializer;

import java.util.List;

public class ResponseWriterHandler implements Handler {

    final Handler next;

    public ResponseWriterHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        if (context.getRequestedData().isPresent()) {
            List<SpeedyEntity> speedyEntities = context.getRequestedData().get();

            IResponseContext responseContext = IResponseContext.builder()
                    .request(context.getHttpServletRequest())
                    .response(context.getHttpServletResponse())
                    .metaModelProcessor(context.getMetaModel())
                    .entityMetadata(context.getEntityMetadata())
                    .expands(context.getExpands())
                    .build();

            IResponseSerializer jsonSerializer = new JSONSerializer(responseContext);
            jsonSerializer.write(speedyEntities);
        } else {
            throw new InternalServerError("Failed to generate Response");
        }
        next.process(context);
    }
}
