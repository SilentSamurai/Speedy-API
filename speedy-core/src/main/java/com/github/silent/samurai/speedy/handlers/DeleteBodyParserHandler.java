package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.request.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.context.SpeedyContext;

/// Parses the raw body of a $delete request into a SpeedyDeleteBody, using the
/// IRequestBodyParser the factory selected during content negotiation.
///
/// Single-purpose: it does not decide which body to parse — that dispatch lives in
/// {@code SpeedyFactory.processReqV2}'s request-type switch, which routes DELETE here.
///
/// @see IRequestBodyParser
/// @see DeleteHandler
public class DeleteBodyParserHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        byte[] rawBody = context.get(byte[].class);
        IRequestBodyParser parser = context.get(IRequestBodyParser.class);
        SpeedyUriContext uriContext = context.get(SpeedyUriContext.class);

        SpeedyBody body = parser.parseDelete(rawBody,
                uriContext.getParsedQuery().getFrom(),
                context.get(TransactionMode.class),
                context.get(QueryProcessor.class));

        context.put(SpeedyBody.class, body);
    }
}
