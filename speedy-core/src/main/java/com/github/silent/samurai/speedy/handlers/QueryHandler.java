package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

import java.math.BigInteger;

/// Handles POST /{Entity}/$query requests with JSON body DSL.
///
/// Reads the SpeedyQuery (parsed from the JSON body by DefaultRequestParser and set as
/// body by QueryBodyParserHandler), executes the query with count, and produces
/// SpeedyEntityResponse or SpeedyCountResponse.
///
/// @see QueryBodyParserHandler
/// @see com.github.silent.samurai.speedy.serialization.DefaultRequestParser
public class QueryHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyQuery speedyQuery = (SpeedyQuery) context.get(SpeedyBody.class);
        EntityMetadata resourceMetadata = speedyQuery.getFrom();
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);

        if (speedyQuery.isCountRequest()) {
            BigInteger count = queryProcessor.executeCount(speedyQuery);
            context.put(SpeedyResponse.class, ResponseBuilders.countResponse(count));
        } else {
            QueryResult result = queryProcessor.executeManyWithCount(speedyQuery);
            context.put(SpeedyResponse.class,
                    ResponseBuilders.entityListResponse(resourceMetadata, result.entities(), speedyQuery, result));
        }
    }
}
