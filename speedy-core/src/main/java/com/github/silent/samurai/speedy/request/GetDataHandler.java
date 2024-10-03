package com.github.silent.samurai.speedy.request;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.util.List;
import java.util.Optional;

public class GetDataHandler {

    private final IRequestContextImpl context;

    public GetDataHandler(IRequestContextImpl context) {
        this.context = context;
    }

//    public Optional<SpeedyEntity> processOne(QueryProcessor queryProcessor) throws Exception {
//        SpeedyQuery speedyQuery = context.getSpeedyQuery();
//        SpeedyEntity result = queryProcessor.executeOne(speedyQuery);
//        return Optional.ofNullable(result);
//    }

    public Optional<List<SpeedyEntity>> processMany(SpeedyQuery speedyQuery) throws Exception {
        EntityMetadata resourceMetadata = speedyQuery.getFrom();
        if (!resourceMetadata.isReadAllowed()) {
            throw new BadRequestException(String.format("read not allowed for %s", resourceMetadata.getName()));
        }

        QueryProcessor queryProcessor = context.getQueryProcessor();
        List<SpeedyEntity> result = queryProcessor.executeMany(speedyQuery);
        return Optional.ofNullable(result);
    }
}
