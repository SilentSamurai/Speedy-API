package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;

import java.util.List;
import java.util.Optional;

public class GetHandler implements Handler {

    final Handler next;

    public GetHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        EntityMetadata resourceMetadata = context.getEntityMetadata();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        SpeedyQuery speedyQuery = context.getSpeedyQuery();

        List<SpeedyEntity> result = queryProcessor.executeMany(speedyQuery);

        Optional<List<SpeedyEntity>> requestedData = Optional.ofNullable(result);
        if (requestedData.isEmpty()) {
            throw new NotFoundException("Not data found for " + resourceMetadata.getName());
        }

        context.setResponseSerializer(new JSONSerializerV2(
                requestedData.get(),
                speedyQuery.getPageInfo().getPageNo(),
                speedyQuery.getExpand()
        ));

        next.process(context);
    }
}
