package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.RequestContext;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.request.GetDataHandler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GetHandler implements Handler {

    final Handler next;

    public GetHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        MetaModel metaModel = context.getMetaModel();
        String requestURI = context.getRequestUri();
        EntityMetadata resourceMetadata = context.getEntityMetadata();
        QueryProcessor queryProcessor = context.getQueryProcessor();

        SpeedyUriContext parser = new SpeedyUriContext(metaModel, requestURI);
        SpeedyQuery speedyQuery = parser.parse();

        SpeedyQueryImpl speedyImpl = (SpeedyQueryImpl) speedyQuery;
        speedyImpl.setExpand(
                speedyQuery.getFrom()
                        .getAssociatedFields().stream().map(
                                item -> item.getAssociationMetadata().getName()
                        ).collect(Collectors.toList())
        );

        if (!resourceMetadata.isReadAllowed()) {
            throw new BadRequestException(String.format("read not allowed for %s", resourceMetadata.getName()));
        }

        List<SpeedyEntity> result = queryProcessor.executeMany(speedyQuery);

        Optional<List<SpeedyEntity>> requestedData = Optional.ofNullable(result);
        if (requestedData.isEmpty()) {
            throw new NotFoundException();
        }
        context.setExpands(speedyImpl.getExpand());
        context.setSpeedyQuery(speedyQuery);
        context.setRequestedData(requestedData);

        next.process(context);
    }
}
