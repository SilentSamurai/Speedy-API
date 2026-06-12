package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.handlers.*;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.mappings.ConversionContext;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.request.SpeedyRequest;
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
    /// The conversion context carrying registries for type conversion throughout
    /// the request pipeline. Passed to every {@link RequestContext} instance.
    ///
    /// @see ConversionContext
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
                            ConversionContext conversionContext) {
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

        parserSelectionChain = List.of(
                new HeadHandler(),
                new ParserSelectionHandler(),
                new TailHandler()
        );

        serializerSelectionChain = List.of(
                new HeadHandler(),
                new SerializerSelectionHandler(),
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

    private RequestContext newContext(HttpServletRequest request, HttpServletResponse response) {
        return new RequestContext(config, dialect, metaModel, request, response, eventProcessor, validationProcessor, conversionContext);
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
    public SpeedyRequest parseRequest(HttpServletRequest request) throws SpeedyHttpException {
        RequestContext ctx = newContext(request, null);
        run(requestChain, ctx);
        SpeedyRequest req = ctx.getRequest();
        req.setRequestType(ctx.getRequestType());
        req.setRawBody(ctx.getRawBody());
        return req;
    }

    @Override
    public IRequestBodyParser selectBodyParser(SpeedyRequest request) throws SpeedyHttpException {
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        run(parserSelectionChain, ctx);
        return ctx.getRequestBodyParser();
    }

    @Override
    public SpeedyBody parseBody(IRequestBodyParser parser, SpeedyRequest request, QueryProcessor qp) throws SpeedyHttpException {
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setRawBody(request.getRawBody());
        ctx.setRequestBodyParser(parser);
        ctx.setRequestType(request.getRequestType());
        ctx.setQueryProcessor(qp);
        run(bodyChain, ctx);
        return ctx.getRequest().getBody();
    }

    @Override
    public SpeedyResponse get(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException {
        request.setBody(body);
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setQueryProcessor(qp);
        run(getChain, ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse query(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException {
        request.setBody(body);
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setQueryProcessor(qp);
        run(queryChain, ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse create(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException {
        request.setBody(body);
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setQueryProcessor(qp);
        run(createChain, ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse update(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException {
        request.setBody(body);
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setQueryProcessor(qp);
        run(updateChain, ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse delete(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException {
        request.setBody(body);
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setQueryProcessor(qp);
        run(deleteChain, ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public IResponseSerializerV2 selectSerializer(HttpServletRequest servletRequest, SpeedyRequest speedyRequest) throws SpeedyHttpException {
        RequestContext ctx = newContext(servletRequest, null);
        ctx.setRequest(speedyRequest);
        run(serializerSelectionChain, ctx);
        return ctx.getResponseSerializer();
    }

    @Override
    public void writeResponse(IResponseSerializerV2 serializer, SpeedyResponse response, HttpServletResponse servletResponse) throws SpeedyHttpException {
        RequestContext ctx = newContext(null, servletResponse);
        ctx.setResponseSerializer(serializer);
        ctx.setSpeedyResponse(response);
        run(responseChain, ctx);
    }
}
