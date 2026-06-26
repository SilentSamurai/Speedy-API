package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.Handler;
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
import com.github.silent.samurai.speedy.validation.ValidationProcessor;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

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
            context.put(SpeedyResponse.class,
                    SpeedyCountResponse.builder()
                            .count(count)
                            .status(200)
                            .build()
            );
            return;
        }

        QueryResult result = queryProcessor.executeManyWithCount(speedyQuery);
        List<SpeedyEntity> entities = result.entities();

        Predicate<FieldMetadata> fieldPredicate = FieldPredicates.buildFieldPredicate(speedyQuery.getSelect());
        context.put(SpeedyResponse.class,
                SpeedyEntityResponse.builder()
                        .entityMetadata(context.get(SpeedyUriContext.class).getParsedQuery().getFrom())
                        .payload(entities)
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
