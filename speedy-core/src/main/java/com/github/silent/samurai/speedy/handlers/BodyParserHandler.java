package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.context.SpeedyContext;

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

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyUriContext uriContext = context.get(SpeedyUriContext.class);
        byte[] rawBody = context.get(byte[].class);
        IRequestBodyParser parser = context.get(IRequestBodyParser.class);
        SpeedyRequestType requestType = context.get(SpeedyRequestType.class);

        SpeedyBody body = switch (requestType) {
            case GET_LIST -> SpeedyBody.empty(SpeedyRequestType.GET_LIST);
            case METADATA -> SpeedyBody.empty(SpeedyRequestType.METADATA);
            case QUERY -> parser.parseQuery(rawBody,
                    context.get(MetaModel.class),
                    uriContext.getParsedQuery(),
                    context.get(ISpeedyConfiguration.class).getMaxPageSize(),
                    context.get(ISpeedyConfiguration.class).getDefaultPageSize());
            case CREATE -> parser.parseCreate(rawBody,
                    context.get(SpeedyUriContext.class).getParsedQuery().getFrom(),
                    context.get(TransactionMode.class),
                    context.get(QueryProcessor.class));
            case UPDATE -> parser.parseUpdate(rawBody,
                    context.get(SpeedyUriContext.class).getParsedQuery().getFrom(),
                    context.get(QueryProcessor.class));
            case DELETE -> parser.parseDelete(rawBody,
                    context.get(SpeedyUriContext.class).getParsedQuery().getFrom(),
                    context.get(TransactionMode.class),
                    context.get(QueryProcessor.class));

        };

        context.put(SpeedyBody.class, body);
    }
}
