package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyCountResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.http.response.FieldPredicates;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

/// Handles POST /{Entity}/$query requests with JSON body DSL.
///
/// Reads the SpeedyQuery (parsed from JSON body by JSONBodyParser and set as
/// body by BodyParserHandler), executes the query with count, and produces
/// SpeedyEntityResponse or SpeedyCountResponse.
///
/// @see BodyParserHandler
/// @see JSONBodyParser
public class QueryHandler implements Handler {

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        SpeedyQuery speedyQuery = (SpeedyQuery) context.get(SpeedyBody.class);
        EntityMetadata resourceMetadata = speedyQuery.getFrom();
        QueryProcessor queryProcessor = context.get(QueryProcessor.class);

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
            List<SpeedyEntity> speedyEntities = result.getEntities();
            Predicate<FieldMetadata> fieldPredicate = FieldPredicates.buildFieldPredicate(speedyQuery.getSelect());
            context.put(SpeedyResponse.class,
                    SpeedyEntityResponse.builder()
                            .payload(speedyEntities)
                            .pageIndex(speedyQuery.getPageInfo().getPageNo())
                            .expands(speedyQuery.getExpand())
                            .totalCount(result.getTotalCount())
                            .requestedPageSize(speedyQuery.getPageInfo().getPageSize())
                            .fieldPredicate(fieldPredicate)
                            .status(200)
                            .build()
            );
        }
    }
}
