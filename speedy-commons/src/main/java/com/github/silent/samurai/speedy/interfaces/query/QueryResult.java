package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.math.BigInteger;
import java.util.List;

public class QueryResult {

    private final List<SpeedyEntity> entities;
    private final BigInteger totalCount;

    public QueryResult(List<SpeedyEntity> entities, BigInteger totalCount) {
        this.entities = entities;
        this.totalCount = totalCount;
    }

    public List<SpeedyEntity> getEntities() {
        return entities;
    }

    public BigInteger getTotalCount() {
        return totalCount;
    }
}
