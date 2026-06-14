package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.handlers.*;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedyEngineImpl implements SpeedyEngine {

    private final ISpeedyConfiguration config;
    private final SpeedyDialect dialect;
    private final MetaModel metaModel;
    private final EventProcessor eventProcessor;
    private final ValidationProcessor validationProcessor;
    private final ConversionContext conversionContext;

    private final List<Handler> uriChain;
    private final List<Handler> headerChain;
    private final List<Handler> operationChain;
    private final List<Handler> queryBodyChain;
    private final List<Handler> createBodyChain;
    private final List<Handler> updateBodyChain;
    private final List<Handler> deleteBodyChain;
    private final List<Handler> parserSelectionChain;
    private final List<Handler> serializerSelectionChain;
    private final List<Handler> getChain;
    private final List<Handler> queryChain;
    private final List<Handler> createChain;
    private final List<Handler> updateChain;
    private final List<Handler> deleteChain;
    private final List<Handler> metadataChain;
    private final ConcurrentHashMap<DataSource, QueryProcessor> queryProcessorCache = new ConcurrentHashMap<>();

    public SpeedyEngineImpl(ISpeedyConfiguration config,
                            SpeedyDialect dialect,
                            MetaModel metaModel,
                            EventProcessor eventProcessor,
                            ValidationProcessor validationProcessor,
                            long maxRequestBodySize,
                            ConversionContext conversionContext,
                            ContentNegotiationManager negotiationManager) {
        this.config = config;
        this.dialect = dialect;
        this.metaModel = metaModel;
        this.eventProcessor = eventProcessor;
        this.validationProcessor = validationProcessor;
        this.conversionContext = conversionContext;

        uriChain = List.of(
                new HeadHandler(),
                new UriParserHandler(),
                new TailHandler()
        );

        headerChain = List.of(
                new HeadHandler(),
                new RequestParserHandler(maxRequestBodySize),
                new TailHandler()
        );

        operationChain = List.of(
                new HeadHandler(),
                new OperationResolverHandler(),
                new TailHandler()
        );

        queryBodyChain = List.of(
                new HeadHandler(),
                new QueryBodyParserHandler(),
                new TailHandler()
        );
        createBodyChain = List.of(
                new HeadHandler(),
                new CreateBodyParserHandler(),
                new TailHandler()
        );
        updateBodyChain = List.of(
                new HeadHandler(),
                new UpdateBodyParserHandler(),
                new TailHandler()
        );
        deleteBodyChain = List.of(
                new HeadHandler(),
                new DeleteBodyParserHandler(),
                new TailHandler()
        );

        parserSelectionChain = List.of(
                new HeadHandler(),
                new ParserSelectionHandler(negotiationManager),
                new TailHandler()
        );

        serializerSelectionChain = List.of(
                new HeadHandler(),
                new SerializerSelectionHandler(negotiationManager),
                new TailHandler()
        );

        getChain = List.of(
                new HeadHandler(),
                new PermissionCheckHandler(PermissionType.READ),
                new GetHandler(),
                new TailHandler()
        );
        queryChain = List.of(
                new HeadHandler(),
                new PermissionCheckHandler(PermissionType.READ),
                new QueryHandler(),
                new TailHandler()
        );
        createChain = List.of(
                new HeadHandler(),
                new PermissionCheckHandler(PermissionType.CREATE),
                new CreateHandler(),
                new TailHandler()
        );
        updateChain = List.of(
                new HeadHandler(),
                new PermissionCheckHandler(PermissionType.UPDATE),
                new UpdateHandler(),
                new TailHandler()
        );
        deleteChain = List.of(
                new HeadHandler(),
                new PermissionCheckHandler(PermissionType.DELETE),
                new DeleteHandler(),
                new TailHandler()
        );

        metadataChain = List.of(
                new HeadHandler(),
                new MetadataHandler(),
                new TailHandler()
        );

    }

    @Override
    public SpeedyContext newContext(HttpServletRequest request, HttpServletResponse response) {
        SpeedyContext ctx = new SpeedyContext();
        ctx.put(ISpeedyConfiguration.class, config);
        ctx.put(dialect);
        ctx.put(MetaModel.class, metaModel);
        ctx.put(eventProcessor);
        ctx.put(validationProcessor);
        ctx.put(conversionContext);
        if (request != null) ctx.put(HttpServletRequest.class, request);
        if (response != null) ctx.put(HttpServletResponse.class, response);
        return ctx;
    }

    private void run(List<Handler> chain, SpeedyContext ctx) throws SpeedyHttpException {
        for (Handler handler : chain) {
            handler.process(ctx);
        }
    }

    @Override
    public void prepare(SpeedyContext ctx) throws SpeedyHttpException {
        DataSource dataSource = config.dataSourcePerReq();
        ctx.put(QueryProcessor.class, queryProcessorCache.computeIfAbsent(
                dataSource, ds -> config.queryProcessor(ds, dialect, conversionContext)));
    }

    @Override
    public SpeedyUriContext parseUri(SpeedyContext ctx) throws SpeedyHttpException {
        run(uriChain, ctx);
        return ctx.get(SpeedyUriContext.class);
    }

    @Override
    public SpeedyHeaders parseHeaders(SpeedyContext ctx) throws SpeedyHttpException {
        run(headerChain, ctx);
        return ctx.get(SpeedyHeaders.class);
    }

    @Override
    public SpeedyRequestType resolveOperation(SpeedyContext ctx) throws SpeedyHttpException {
        run(operationChain, ctx);
        return ctx.get(SpeedyRequestType.class);
    }

    @Override
    public IRequestBodyParser selectBodyParser(SpeedyContext ctx) throws SpeedyHttpException {
        run(parserSelectionChain, ctx);
        return ctx.get(IRequestBodyParser.class);
    }

    @Override
    public SpeedyBody parseQueryBody(SpeedyContext ctx) throws SpeedyHttpException {
        run(queryBodyChain, ctx);
        return ctx.get(SpeedyBody.class);
    }

    @Override
    public SpeedyBody parseCreateBody(SpeedyContext ctx) throws SpeedyHttpException {
        run(createBodyChain, ctx);
        return ctx.get(SpeedyBody.class);
    }

    @Override
    public SpeedyBody parseUpdateBody(SpeedyContext ctx) throws SpeedyHttpException {
        run(updateBodyChain, ctx);
        return ctx.get(SpeedyBody.class);
    }

    @Override
    public SpeedyBody parseDeleteBody(SpeedyContext ctx) throws SpeedyHttpException {
        run(deleteBodyChain, ctx);
        return ctx.get(SpeedyBody.class);
    }

    @Override
    public SpeedyResponse get(SpeedyContext ctx) throws SpeedyHttpException {
        run(getChain, ctx);
        return ctx.get(SpeedyResponse.class);
    }

    @Override
    public SpeedyResponse query(SpeedyContext ctx) throws SpeedyHttpException {
        run(queryChain, ctx);
        return ctx.get(SpeedyResponse.class);
    }

    @Override
    public SpeedyResponse create(SpeedyContext ctx) throws SpeedyHttpException {
        run(createChain, ctx);
        return ctx.get(SpeedyResponse.class);
    }

    @Override
    public SpeedyResponse update(SpeedyContext ctx) throws SpeedyHttpException {
        run(updateChain, ctx);
        return ctx.get(SpeedyResponse.class);
    }

    @Override
    public SpeedyResponse delete(SpeedyContext ctx) throws SpeedyHttpException {
        run(deleteChain, ctx);
        return ctx.get(SpeedyResponse.class);
    }

    @Override
    public SpeedyResponse metadata(SpeedyContext ctx) throws SpeedyHttpException {
        run(metadataChain, ctx);
        return ctx.get(SpeedyResponse.class);
    }

    @Override
    public IResponseSerializerV2 selectSerializer(SpeedyContext ctx) throws SpeedyHttpException {
        run(serializerSelectionChain, ctx);
        return ctx.get(IResponseSerializerV2.class);
    }

}
