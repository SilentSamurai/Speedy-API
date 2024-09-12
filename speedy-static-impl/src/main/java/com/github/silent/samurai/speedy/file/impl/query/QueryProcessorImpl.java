package com.github.silent.samurai.speedy.file.impl.query;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.util.List;

public class QueryProcessorImpl implements QueryProcessor {
    @Override
    public SpeedyEntity executeOne(SpeedyQuery query) throws SpeedyHttpException {
        return null;
    }

    @Override
    public List<SpeedyEntity> executeMany(SpeedyQuery query) throws SpeedyHttpException {
        return List.of();
    }

    @Override
    public boolean exists(SpeedyEntityKey entityKey) throws SpeedyHttpException {
        return false;
    }

    @Override
    public SpeedyEntity create(SpeedyEntity entity) throws SpeedyHttpException {
        return null;
    }

    @Override
    public SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        return null;
    }

    @Override
    public SpeedyEntity delete(SpeedyEntityKey entityKey) throws SpeedyHttpException {
        return null;
    }
}
