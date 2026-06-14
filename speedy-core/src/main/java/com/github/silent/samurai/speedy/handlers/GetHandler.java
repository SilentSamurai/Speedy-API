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
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.http.response.FieldPredicates;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

/// Handles GET /{Entity} requests using the URI-parsed SpeedyQuery body.
///
/// Reads the SpeedyQuery (set as body by BodyParserHandler for GET_LIST),
/// executes the query with count, and produces the appropriate SpeedyEntityResponse
/// or SpeedyCountResponse for the response serializer.
///
/// @see BodyParserHandler
/// @see SpeedyQuery
public class GetHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
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
            return;
        }

        QueryResult result = queryProcessor.executeManyWithCount(speedyQuery);
        List<SpeedyEntity> entities = result.getEntities();

        Predicate<FieldMetadata> fieldPredicate = FieldPredicates.buildFieldPredicate(speedyQuery.getSelect());
        context.put(SpeedyResponse.class,
                SpeedyEntityResponse.builder()
                        .entityMetadata(context.get(SpeedyUriContext.class).getParsedQuery().getFrom())
                        .payload(entities)
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
