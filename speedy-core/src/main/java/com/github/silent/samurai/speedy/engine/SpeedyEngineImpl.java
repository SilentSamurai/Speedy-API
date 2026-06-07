package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.handlers.*;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.query.JooqQueryProcessorImpl;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.request.SpeedyRequest;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedyEngineImpl implements SpeedyEngine {

    private final ISpeedyConfiguration config;
    private final SpeedyDialect dialect;
    private final MetaModel metaModel;
    private final EventProcessor eventProcessor;
    private final ValidationProcessor validationProcessor;

    private final Handler requestChain;
    private final Handler bodyChain;
    private final Handler parserSelectionChain;
    private final Handler serializerSelectionChain;
    private final Handler getChain;
    private final Handler queryChain;
    private final Handler createChain;
    private final Handler updateChain;
    private final Handler deleteChain;
    private final Handler responseChain;
    private final ConcurrentHashMap<DataSource, QueryProcessor> queryProcessorCache = new ConcurrentHashMap<>();

    public SpeedyEngineImpl(ISpeedyConfiguration config,
                            SpeedyDialect dialect,
                            MetaModel metaModel,
                            EventProcessor eventProcessor,
                            ValidationProcessor validationProcessor,
                            long maxRequestBodySize) {
        this.config = config;
        this.dialect = dialect;
        this.metaModel = metaModel;
        this.eventProcessor = eventProcessor;
        this.validationProcessor = validationProcessor;

        TailHandler tail = new TailHandler();

        requestChain = new HeadHandler(
                new RequestParserHandler(
                        new UriParserHandler(
                                new OperationResolverHandler(tail)),
                        maxRequestBodySize));

        bodyChain = new HeadHandler(new BodyParserHandler(tail));

        parserSelectionChain = new HeadHandler(new ParserSelectionHandler(tail));

        serializerSelectionChain = new HeadHandler(new SerializerSelectionHandler(tail));

        getChain = new HeadHandler(new PermissionCheckHandler(
                new GetHandler(tail), PermissionType.READ));
        queryChain = new HeadHandler(new PermissionCheckHandler(
                new QueryHandler(tail), PermissionType.READ));
        createChain = new HeadHandler(new PermissionCheckHandler(
                new CreateHandler(tail), PermissionType.CREATE));
        updateChain = new HeadHandler(new PermissionCheckHandler(
                new UpdateHandler(tail), PermissionType.UPDATE));
        deleteChain = new HeadHandler(new PermissionCheckHandler(
                new DeleteHandler(tail), PermissionType.DELETE));

        responseChain = new HeadHandler(new SpeedyResponseWriterHandler(tail));
    }

    private RequestContext newContext(HttpServletRequest request, HttpServletResponse response) {
        return new RequestContext(config, dialect, metaModel, request, response, eventProcessor, validationProcessor);
    }

    @Override
    public QueryProcessor prepare() throws SpeedyHttpException {
        DataSource dataSource = config.dataSourcePerReq();
        return queryProcessorCache.computeIfAbsent(
                dataSource, ds -> new JooqQueryProcessorImpl(ds, dialect));
    }

    @Override
    public SpeedyRequest parseRequest(HttpServletRequest request) throws SpeedyHttpException {
        RequestContext ctx = newContext(request, null);
        requestChain.process(ctx);
        SpeedyRequest req = ctx.getRequest();
        req.setRequestType(ctx.getRequestType());
        req.setRawBody(ctx.getRawBody());
        return req;
    }

    @Override
    public IRequestBodyParser selectBodyParser(SpeedyRequest request) throws SpeedyHttpException {
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        parserSelectionChain.process(ctx);
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
        bodyChain.process(ctx);
        return ctx.getRequest().getBody();
    }

    @Override
    public SpeedyResponse get(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException {
        request.setBody(body);
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setQueryProcessor(qp);
        getChain.process(ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse query(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException {
        request.setBody(body);
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setQueryProcessor(qp);
        queryChain.process(ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse create(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException {
        request.setBody(body);
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setQueryProcessor(qp);
        createChain.process(ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse update(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException {
        request.setBody(body);
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setQueryProcessor(qp);
        updateChain.process(ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse delete(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException {
        request.setBody(body);
        RequestContext ctx = newContext(null, null);
        ctx.setRequest(request);
        ctx.setQueryProcessor(qp);
        deleteChain.process(ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public IResponseSerializerV2 selectSerializer(HttpServletRequest servletRequest, SpeedyRequest speedyRequest) throws SpeedyHttpException {
        RequestContext ctx = newContext(servletRequest, null);
        ctx.setRequest(speedyRequest);
        serializerSelectionChain.process(ctx);
        return ctx.getResponseSerializer();
    }

    @Override
    public void writeResponse(IResponseSerializerV2 serializer, SpeedyResponse response, HttpServletResponse servletResponse) throws SpeedyHttpException {
        RequestContext ctx = newContext(null, servletResponse);
        ctx.setResponseSerializer(serializer);
        ctx.setSpeedyResponse(response);
        responseChain.process(ctx);
    }
}
