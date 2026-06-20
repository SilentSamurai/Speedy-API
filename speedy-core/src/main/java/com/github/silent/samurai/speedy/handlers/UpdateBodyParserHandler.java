package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.context.SpeedyContext;

/// Parses the raw body of an $update request into a SpeedyUpdateBody, using the
/// IRequestBodyParser the factory selected during content negotiation.
///
/// Single-purpose: it does not decide which body to parse — that dispatch lives in
/// {@code SpeedyFactory.processReqV2}'s request-type switch, which routes UPDATE here.
///
/// @see IRequestBodyParser
/// @see UpdateHandler
public class UpdateBodyParserHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        byte[] rawBody = context.get(byte[].class);
        IRequestBodyParser parser = context.get(IRequestBodyParser.class);
        SpeedyUriContext uriContext = context.get(SpeedyUriContext.class);

        SpeedyBody body = parser.parseUpdate(rawBody,
                uriContext.getParsedQuery().getFrom(),
                context.get(QueryProcessor.class));

        context.put(SpeedyBody.class, body);
    }
}
