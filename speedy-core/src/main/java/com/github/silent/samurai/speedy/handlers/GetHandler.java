package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.FieldPredicates;
import com.github.silent.samurai.speedy.serializers.JSONCountSerializerV2;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

/// # GetHandler
///
/// Handles {@code GET /{Entity}} requests. Uses the {@link SpeedyQuery} built by
/// {@link EntityCaptureHandler} from URI query parameters, executes the query
/// with a count, and sets a {@link JSONSerializerV2} on the context.
///
/// ## Purpose
/// - Executes entity list queries from URL query string parameters
/// - Supports count-only requests ({@code $count=true})
/// - Includes pagination metadata (pageIndex, pageSize, totalCount, totalPages)
/// - Applies {@code $select} field filtering via {@link FieldPredicates}
///
/// ## Processing Flow
/// 1. Reads the pre-built {@code SpeedyQuery} from the context
/// 2. If count-only: executes {@code executeCount()} and sets {@link JSONCountSerializerV2}
/// 3. Otherwise: executes {@code executeManyWithCount()} for data + total count
/// 4. Builds a field predicate from {@code $select}
/// 5. Constructs {@link JSONSerializerV2} with entities, page info, total count, and expands
/// 6. Sets the serializer on the context for {@link SpeedyResponseWriterHandler}
///
/// ## Chain Position
/// Dispatched by {@link SwitchHandler} for GET requests. Delegates to the next handler
/// (typically {@link SpeedyResponseWriterHandler}) after setting the serializer.
public class GetHandler implements Handler {

    final Handler next;

    public GetHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        EntityMetadata resourceMetadata = context.getEntityMetadata();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        SpeedyQuery speedyQuery = context.getSpeedyQuery();

        if (speedyQuery.isCountRequest()) {
            BigInteger count = queryProcessor.executeCount(speedyQuery);
            context.setResponseSerializer(new JSONCountSerializerV2(count));
            next.process(context);
            return;
        }

        QueryResult result = queryProcessor.executeManyWithCount(speedyQuery);
        List<SpeedyEntity> entities = result.getEntities();

        Predicate<FieldMetadata> fieldPredicate = FieldPredicates.buildFieldPredicate(speedyQuery.getSelect());
        context.setResponseSerializer(new JSONSerializerV2(
                fieldPredicate,
                entities,
                speedyQuery.getPageInfo().getPageNo(),
                speedyQuery.getExpand(),
                result.getTotalCount(),
                speedyQuery.getPageInfo().getPageSize()
        ));

        next.process(context);
    }
}
