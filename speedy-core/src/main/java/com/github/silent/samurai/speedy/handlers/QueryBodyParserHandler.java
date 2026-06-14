package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.context.SpeedyContext;

/// Parses the raw body of a $query request into a SpeedyQuery (a SpeedyBody),
/// using the IRequestBodyParser the factory selected during content negotiation.
///
/// Single-purpose: it does not decide which body to parse — that dispatch lives in
/// {@code SpeedyFactory.processReqV2}'s request-type switch, which routes QUERY here.
///
/// @see IRequestBodyParser
/// @see QueryHandler
public class QueryBodyParserHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        byte[] rawBody = context.get(byte[].class);
        IRequestBodyParser parser = context.get(IRequestBodyParser.class);
        SpeedyUriContext uriContext = context.get(SpeedyUriContext.class);

        SpeedyBody body = parser.parseQuery(rawBody,
                context.get(MetaModel.class),
                uriContext.getParsedQuery(),
                context.get(ISpeedyConfiguration.class).getMaxPageSize(),
                context.get(ISpeedyConfiguration.class).getDefaultPageSize());

        context.put(SpeedyBody.class, body);
    }
}
