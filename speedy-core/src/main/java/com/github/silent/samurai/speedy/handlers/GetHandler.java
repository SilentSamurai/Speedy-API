package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;

import java.math.BigInteger;

/// Handles GET /{Entity} requests using the URI-parsed SpeedyQuery (parsed by UriParserHandler).
/// Executes the query with count and produces the appropriate SpeedyEntityResponse
/// or SpeedyCountResponse for the response serializer.
///
/// @see UriParserHandler
/// @see SpeedyQuery
public class GetHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyQuery speedyQuery = context.get(SpeedyUriContext.class).getParsedQuery();
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);

        context.get(ValidationProcessor.class).validateQueryRequest(speedyQuery);

        if (speedyQuery.isCountRequest()) {
            BigInteger count = queryProcessor.executeCount(speedyQuery);
            context.put(SpeedyResponse.class, ResponseBuilders.countResponse(count));
            return;
        }

        QueryResult result = queryProcessor.executeManyWithCount(speedyQuery);
        context.put(SpeedyResponse.class,
                ResponseBuilders.entityListResponse(speedyQuery.getFrom(), result.entities(), speedyQuery, result));
    }
}
