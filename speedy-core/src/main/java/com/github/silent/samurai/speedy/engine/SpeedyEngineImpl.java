package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.handlers.*;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedyEngineImpl implements SpeedyEngine {

    private final ISpeedyConfiguration config;
    private final SpeedyDialect dialect;
    private final MetaModel metaModel;
    private final EventProcessor eventProcessor;
    private final ValidationProcessor validationProcessor;
    private final ConversionContext conversionContext;

    private final List<Handler> requestChain;
    private final List<Handler> bodyChain;
    private final List<Handler> parserSelectionChain;
    private final List<Handler> serializerSelectionChain;
    private final List<Handler> getChain;
    private final List<Handler> queryChain;
    private final List<Handler> createChain;
    private final List<Handler> updateChain;
    private final List<Handler> deleteChain;
    private final List<Handler> responseChain;
    private final ConcurrentHashMap<DataSource, QueryProcessor> queryProcessorCache = new ConcurrentHashMap<>();

    public SpeedyEngineImpl(ISpeedyConfiguration config,
                            SpeedyDialect dialect,
                            MetaModel metaModel,
                            EventProcessor eventProcessor,
                            ValidationProcessor validationProcessor,
                            long maxRequestBodySize,
                            ConversionContext conversionContext,
                            Map<String, IRequestBodyParserProvider> parserProviders,
                            Map<String, IResponseSerializerProvider> serializerProviders) {
        this.config = config;
        this.dialect = dialect;
        this.metaModel = metaModel;
        this.eventProcessor = eventProcessor;
        this.validationProcessor = validationProcessor;
        this.conversionContext = conversionContext;

        requestChain = List.of(
                new HeadHandler(),
                new RequestParserHandler(maxRequestBodySize),
                new UriParserHandler(),
                new OperationResolverHandler(),
                new TailHandler()
        );

        bodyChain = List.of(
                new HeadHandler(),
                new BodyParserHandler(),
                new TailHandler()
        );

        ContentNegotiationManager negotiationManager = new ContentNegotiationManager(parserProviders, serializerProviders);

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

        responseChain = List.of(
                new HeadHandler(),
                new SpeedyResponseWriterHandler(),
                new TailHandler()
        );
    }

    @Override
    public RequestContext newContext(HttpServletRequest request, HttpServletResponse response) {
        RequestContext ctx = new RequestContext();
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

    private void run(List<Handler> chain, RequestContext ctx) throws SpeedyHttpException {
        for (Handler handler : chain) {
            handler.process(ctx);
        }
    }

    @Override
    public QueryProcessor prepare() throws SpeedyHttpException {
        DataSource dataSource = config.dataSourcePerReq();
        return queryProcessorCache.computeIfAbsent(
                dataSource, ds -> config.queryProcessor(ds, dialect, conversionContext));
    }

    @Override
    public void parseRequest(RequestContext ctx) throws SpeedyHttpException {
        run(requestChain, ctx);
    }

    @Override
    public void selectBodyParser(RequestContext ctx) throws SpeedyHttpException {
        run(parserSelectionChain, ctx);
    }

    @Override
    public void parseBody(RequestContext ctx) throws SpeedyHttpException {
        run(bodyChain, ctx);
    }

    @Override
    public void execute(RequestContext ctx) throws SpeedyHttpException {
        SpeedyRequestType type = ctx.get(SpeedyRequestType.class);
        switch (type) {
            case GET_LIST -> run(getChain, ctx);
            case QUERY -> run(queryChain, ctx);
            case CREATE -> run(createChain, ctx);
            case UPDATE -> run(updateChain, ctx);
            case DELETE -> run(deleteChain, ctx);
        }
    }

    @Override
    public void selectSerializer(RequestContext ctx) throws SpeedyHttpException {
        run(serializerSelectionChain, ctx);
    }

    @Override
    public void writeResponse(RequestContext ctx) throws SpeedyHttpException {
        run(responseChain, ctx);
    }
}
