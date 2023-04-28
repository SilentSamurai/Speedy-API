package com.github.silent.samurai.request.get;

import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.parser.SpeedyUriParser;
import com.github.silent.samurai.query.QueryBuilder;

import javax.persistence.Query;
import java.util.Optional;

public class GetDataHandler {

    private final GetRequestContext context;

    public GetDataHandler(GetRequestContext context) {
        this.context = context;
    }

    public Optional<Object> process() throws Exception {
        Object requestObject;
        SpeedyUriParser parser = context.getParser();
        EntityMetadata entityMetadata = context.getResourceMetadata();
        if (parser.isOnlyIdentifiersPresent()) {
            Object pk = MetadataUtil.createIdentifierFromParser(parser);
            requestObject = context.getEntityManager().find(entityMetadata.getEntityClass(), pk);
        } else {
            QueryBuilder queryBuilder = new QueryBuilder(parser, entityMetadata, context.getEntityManager());
            Query query = queryBuilder.getQuery();
            requestObject = query.getResultList();
        }
        return Optional.ofNullable(requestObject);
    }
}
