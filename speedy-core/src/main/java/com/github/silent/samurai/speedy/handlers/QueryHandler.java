package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyCountResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.serialization.DefaultRequestParser;
import com.github.silent.samurai.speedy.serialization.FieldPredicates;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

/// Handles POST /{Entity}/$query requests with JSON body DSL.
///
/// Reads the SpeedyQuery (parsed from the JSON body by DefaultRequestParser and set as
/// body by QueryBodyParserHandler), executes the query with count, and produces
/// SpeedyEntityResponse or SpeedyCountResponse.
///
/// @see QueryBodyParserHandler
/// @see DefaultRequestParser
public class QueryHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyQuery speedyQuery = (SpeedyQuery) context.get(SpeedyBody.class);
        EntityMetadata resourceMetadata = speedyQuery.getFrom();
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);

        context.get(ValidationProcessor.class).validateQueryRequest(speedyQuery);

        if (speedyQuery.isCountRequest()) {
            BigInteger count = queryProcessor.executeCount(speedyQuery);
            context.put(SpeedyResponse.class,
                    SpeedyCountResponse.builder()
                            .count(count)
                            .status(200)
                            .build()
            );
        } else {
            QueryResult result = queryProcessor.executeManyWithCount(speedyQuery);
            List<SpeedyEntity> speedyEntities = result.entities();
            Predicate<FieldMetadata> fieldPredicate = FieldPredicates.buildFieldPredicate(speedyQuery.getSelect());
            context.put(SpeedyResponse.class,
                    SpeedyEntityResponse.builder()
                            .entityMetadata(context.get(SpeedyUriContext.class).getParsedQuery().getFrom())
                            .payload(speedyEntities)
                            .pageIndex(speedyQuery.getPageInfo().getPageNo())
                            .expands(speedyQuery.getExpand())
                            .totalCount(result.totalCount())
                            .requestedPageSize(speedyQuery.getPageInfo().getPageSize())
                            .fieldPredicate(fieldPredicate)
                            .status(200)
                            .build()
            );
        }
    }
}
