package com.github.silent.samurai.speedy.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.query.JsonQueryBuilder;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.JSONCountSerializerV2;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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

        JsonQueryBuilder jsonQueryBuilder = new JsonQueryBuilder(metaModel, resourceMetadata, jsonBody);
        SpeedyQuery speedyQuery = jsonQueryBuilder.build();
        context.setSpeedyQuery(speedyQuery);

        if (speedyQuery.getSelect().contains("count")) {
            BigInteger count = queryProcessor.executeCount(speedyQuery);
            context.setResponseSerializer(new JSONCountSerializerV2(
                    count
            ));
        } else {
            List<SpeedyEntity> speedyEntities = queryProcessor.executeMany(speedyQuery);
            context.setResponseSerializer(new JSONSerializerV2(
                    speedyEntities,
                    speedyQuery.getPageInfo().getPageNo(),
                    speedyQuery.getExpand()
            ));
        }
        next.process(context);
    }
}
