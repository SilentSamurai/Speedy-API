package com.github.silent.samurai.speedy.request.get;

import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.util.List;
import java.util.Optional;

public class GetDataHandler {

    private final GetRequestContext context;

    public GetDataHandler(GetRequestContext context) {
        this.context = context;
    }

    public Optional<SpeedyEntity> processOne() throws Exception {
        QueryProcessor queryProcessor = context.getMetaModelProcessor()
                .getQueryProcess(context.getEntityManager());
        SpeedyQuery speedyQuery = context.getSpeedyQuery();
        SpeedyEntity result = queryProcessor.executeOne(speedyQuery);
        return Optional.ofNullable(result);
    }

    public Optional<List<SpeedyEntity>> processMany() throws Exception {
        QueryProcessor queryProcessor = context.getMetaModelProcessor()
                .getQueryProcess(context.getEntityManager());
        SpeedyQuery speedyQuery = context.getSpeedyQuery();
        List<SpeedyEntity> result = queryProcessor.executeMany(speedyQuery);
        return Optional.ofNullable(result);
    }
}
