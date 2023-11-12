package com.github.silent.samurai.speedy.request.get;

import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.query.QueryBuilder;

import javax.persistence.Query;
import java.util.Optional;

public class GetDataHandler {

    private final GetRequestContext context;

    public GetDataHandler(GetRequestContext context) {
        this.context = context;
    }

    private Object normalQuery() throws Exception {
        SpeedyUriContext parser = context.getParser();
        QueryBuilder queryBuilder = new QueryBuilder(
                parser.getPrimaryResource(),
                context.getEntityManager());
        queryBuilder.addWhereQuery();
        Query query = queryBuilder.getQuery(parser);
        return query.getResultList();
    }

    public Optional<Object> process() throws Exception {
        Object requestObject;
        SpeedyUriContext parser = context.getParser();
        EntityMetadata entityMetadata = context.getEntityMetadata();
        if (parser.getPrimaryResource().isOnlyIdentifiersPresent()) {
            Object pk = MetadataUtil.createIdentifierFromParser(parser);
            requestObject = context.getEntityManager().find(entityMetadata.getEntityClass(), pk);
        } else {
            requestObject = this.normalQuery();
        }
        return Optional.ofNullable(requestObject);
    }
}
