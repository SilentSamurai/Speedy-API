package com.github.silent.samurai.speedy.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.parser.JsonQueryParser;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.FieldPredicates;
import com.github.silent.samurai.speedy.serializers.JSONCountSerializerV2;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

/// # QueryHandler
///
/// Handles {@code POST /{Entity}/$query} requests. Parses the JSON body
/// ({@code $from}, {@code $where}, {@code $orderBy}, {@code $page},
/// {@code $expand}, {@code $select}) into a {@link SpeedyQuery} via
/// {@link com.github.silent.samurai.speedy.parser.JsonQueryParser}.
///
/// ## Purpose
/// - Provides an advanced query DSL via JSON POST body (richer than URL query params)
/// - Supports complex filter conditions, sorting, pagination, and nested expansions
/// - Supports count-only queries ({@code $count: true})
/// - Applies configurable max page size and default page size limits
///
/// ## Processing Flow
/// 1. Parses the JSON body into a {@code SpeedyQuery} using {@code JsonQueryParser}
/// 2. Sets the parsed query on the context
/// 3. If count-only: executes {@code executeCount()} and sets {@link JSONCountSerializerV2}
/// 4. Otherwise: executes {@code executeManyWithCount()} for data + total count
/// 5. Builds a field predicate from {@code $select}
/// 6. Constructs {@link JSONSerializerV2} with entities, page info, total count, and expands
/// 7. Sets the serializer on the context for {@link SpeedyResponseWriterHandler}
///
/// ## Chain Position
/// Dispatched by {@link SwitchHandler} for POST requests with {@code $query} suffix.
public class QueryHandler implements Handler {

    final Handler next;

    public QueryHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {

        MetaModel metaModel = context.getMetaModel();
        EntityMetadata resourceMetadata = context.getEntityMetadata();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        JsonNode jsonBody = context.getBody();

        JsonQueryParser jsonQueryParser = new JsonQueryParser(metaModel, resourceMetadata, jsonBody);
        jsonQueryParser.setMaxPageSize(context.getConfiguration().getMaxPageSize());
        jsonQueryParser.setDefaultPageSize(context.getConfiguration().getDefaultPageSize());
        SpeedyQuery speedyQuery = jsonQueryParser.build();
        context.setSpeedyQuery(speedyQuery);

        if (speedyQuery.isCountRequest()) {
            BigInteger count = queryProcessor.executeCount(speedyQuery);
            context.setResponseSerializer(new JSONCountSerializerV2(
                    count
            ));
        } else {
            QueryResult result = queryProcessor.executeManyWithCount(speedyQuery);
            List<SpeedyEntity> speedyEntities = result.getEntities();
            Predicate<FieldMetadata> fieldPredicate = FieldPredicates.buildFieldPredicate(speedyQuery.getSelect());
            context.setResponseSerializer(new JSONSerializerV2(
                    fieldPredicate,
                    speedyEntities,
                    speedyQuery.getPageInfo().getPageNo(),
                    speedyQuery.getExpand(),
                    result.getTotalCount(),
                    speedyQuery.getPageInfo().getPageSize()
            ));
        }
        next.process(context);
    }
}
