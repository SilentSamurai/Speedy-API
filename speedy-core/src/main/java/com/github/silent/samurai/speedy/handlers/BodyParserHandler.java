package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.request.SpeedyRequest;

/// Parses the raw request body into the appropriate SpeedyBody subtype.
///
/// Dispatches by SpeedyRequestType: for GET_LIST, wraps the URI-parsed
/// SpeedyQuery as the body; for QUERY, CREATE, UPDATE, and DELETE, delegates
/// to the selected IRequestBodyParser to convert raw bytes into typed body objects.
/// Runs in the body parsing phase, after the factory has selected and set
/// the {@link com.github.silent.samurai.speedy.interfaces.IRequestBodyParser} on the context.
///
/// @see IRequestBodyParser
/// @see SpeedyRequestType
public class BodyParserHandler implements Handler {

    final Handler next;

    public BodyParserHandler(Handler next) {
        this.next = next;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        SpeedyRequest request = context.getRequest();
        byte[] rawBody = context.getRawBody();
        IRequestBodyParser parser = context.getRequestBodyParser();
        SpeedyRequestType requestType = context.getRequestType();

        SpeedyBody body = switch (requestType) {
            case GET_LIST -> {
                SpeedyQueryImpl query = request.getUriContext().getParsedQuery();
                query.setType(SpeedyRequestType.GET_LIST);
                yield query;
            }
            case QUERY -> parser.parseQuery(rawBody,
                    context.getMetaModel(),
                    request.getUriContext().getParsedQuery(),
                    context.getConfiguration().getMaxPageSize(),
                    context.getConfiguration().getDefaultPageSize());
            case CREATE -> parser.parseCreate(rawBody,
                    context.getEntityMetadata(),
                    request.getTransactionMode(),
                    context.getQueryProcessor());
            case UPDATE -> parser.parseUpdate(rawBody,
                    context.getEntityMetadata(),
                    context.getQueryProcessor());
            case DELETE -> parser.parseDelete(rawBody,
                    context.getEntityMetadata(),
                    request.getTransactionMode(),
                    context.getQueryProcessor());
        };

        request.setBody(body);
        next.process(context);
    }
}
