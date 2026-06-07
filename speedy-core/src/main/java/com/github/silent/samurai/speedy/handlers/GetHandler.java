package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyCountResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.FieldPredicates;

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
public class GetHandler implements Handler {

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        SpeedyQuery speedyQuery = (SpeedyQuery) context.getRequest().getBody();
        EntityMetadata resourceMetadata = speedyQuery.getFrom();
        QueryProcessor queryProcessor = context.getQueryProcessor();

        if (speedyQuery.isCountRequest()) {
            BigInteger count = queryProcessor.executeCount(speedyQuery);
            context.setSpeedyResponse(
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
        context.setSpeedyResponse(
                SpeedyEntityResponse.builder()
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
