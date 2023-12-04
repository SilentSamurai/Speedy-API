package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.interfaces.SpeedyEntity;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.stream.Collectors;

public class JpaQueryProcessorImpl implements QueryProcessor {

    final EntityManager entityManager;

    public JpaQueryProcessorImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    public SpeedyEntity executeOne(SpeedyQuery speedyQuery) throws Exception {
        QueryBuilder qb = new QueryBuilder(speedyQuery, entityManager);
        Query query = qb.getQuery();
        List<?> resultList = query.getResultList();
        Object reqObj = resultList.get(0);
        return new JpaSpeedyEntity(reqObj, speedyQuery.getFrom());
    }

    @Override
    public List<SpeedyEntity> executeMany(SpeedyQuery speedyQuery) throws Exception {
        QueryBuilder qb = new QueryBuilder(speedyQuery, entityManager);
        Query query = qb.getQuery();
        List<?> resultList = query.getResultList();
        return resultList.stream().map(e -> new JpaSpeedyEntity(e, speedyQuery.getFrom())).collect(Collectors.toList());
    }




}
