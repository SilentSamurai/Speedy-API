package com.github.silent.samurai.request.get;

import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.parser.SpeedyUriParser;
import com.github.silent.samurai.query.QueryBuilder;

import javax.persistence.Query;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class GetDataHandler {

    private final GetRequestContext context;

    public GetDataHandler(GetRequestContext context) {
        this.context = context;
    }

    private Object getAssociationQuery() throws Exception {
        SpeedyUriParser parser = context.getParser();
        List<Object> resultObject = new LinkedList<>();

        QueryBuilder secondaryQB = new QueryBuilder(
                parser.getSecondaryResource(),
                context.getEntityManager());
        secondaryQB.addWhereQuery();
        Query secondaryQuery = secondaryQB.getQuery(parser);
        List fkObjects = secondaryQuery.getResultList();

        for (Object instance : fkObjects) {
            QueryBuilder queryBuilder = new QueryBuilder(
                    parser.getPrimaryResource(),
                    context.getEntityManager());

            queryBuilder.addWhereQuery();
            queryBuilder.addAssociationFK(
                    parser.getSecondaryResource().getResourceMetadata(),
                    instance
            );

            Query query = queryBuilder.getQuery(parser);
            List resultList = query.getResultList();
            resultObject.addAll(resultList);
        }

        return resultObject;
    }

    private Object normalQuery() throws Exception {
        SpeedyUriParser parser = context.getParser();
        QueryBuilder queryBuilder = new QueryBuilder(
                parser.getPrimaryResource(),
                context.getEntityManager());
        queryBuilder.addWhereQuery();
        Query query = queryBuilder.getQuery(parser);
        return query.getResultList();
    }

    public Optional<Object> process() throws Exception {
        Object requestObject;
        SpeedyUriParser parser = context.getParser();
        EntityMetadata entityMetadata = context.getResourceMetadata();
        if (parser.getPrimaryResource().isOnlyIdentifiersPresent()) {
            Object pk = MetadataUtil.createIdentifierFromParser(parser);
            requestObject = context.getEntityManager().find(entityMetadata.getEntityClass(), pk);
        } else {
            if (parser.isAssociationFilterRequired()) {
                requestObject = this.getAssociationQuery();
            } else {
                requestObject = this.normalQuery();
            }
        }
        return Optional.ofNullable(requestObject);
    }
}
