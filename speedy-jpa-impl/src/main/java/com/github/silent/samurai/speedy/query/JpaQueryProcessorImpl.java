package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyEntity;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.stream.Collectors;

public class JpaQueryProcessorImpl implements QueryProcessor {

    EntityManager entityManager;

    @Override
    public SpeedyEntity executeOne(SpeedyQuery query) {
        return null;
    }

    @Override
    public List<SpeedyEntity> executeMany(SpeedyQuery speedyQuery) throws Exception {
        QueryBuilder qb = new QueryBuilder(speedyQuery, entityManager);
        Query query = qb.getQuery();
        List<?> resultList = query.getResultList();


        return resultList.stream().map(e -> new SpeedyEntity() {
            @Override
            public EntityMetadata getEntityMetadata() {
                return speedyQuery.getFrom();
            }

            @Override
            public Object getEntity() {
                return e;
            }
        }).collect(Collectors.toList());
    }


}
