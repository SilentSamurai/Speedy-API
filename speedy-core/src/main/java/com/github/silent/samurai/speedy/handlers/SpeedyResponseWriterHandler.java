package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.request.IResponseContext;
import com.github.silent.samurai.speedy.request.RequestContext;

/// # SpeedyResponseWriterHandler
///
/// Invokes the response serializer to write the final output to the
/// {@code HttpServletResponse} output stream. Builds an {@link IResponseContext}
/// from the current {@link RequestContext} and passes it to the serializer's
/// {@code write()} method.
///
/// ## Purpose
/// - Decouples response serialization from business logic
/// - Supports any {@link IResponseSerializerV2} implementation set by upstream handlers
/// - Constructs the lightweight {@code IResponseContext} used by serializers
///
/// ## Processing Flow
/// 1. Retrieves the {@code IResponseSerializerV2} set on the context by a prior handler
/// 2. Builds an {@code IResponseContext} with the metamodel, request/response objects,
///    entity metadata, and expand/page metadata
/// 3. Calls {@code serializer.write(responseContext)} to produce the final JSON output
/// 4. Delegates to the next handler (TailHandler)
///
/// ## Chain Position
/// Second-to-last handler. Always delegates to {@link TailHandler} after writing.
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
