package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.handlers.*;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.query.JooqQueryProcessorImpl;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.request.SpeedyRequest;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedyEngineImpl implements SpeedyEngine {

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
    private final long maxRequestBodySize;

    public SpeedyEngineImpl(long maxRequestBodySize) {
        this.maxRequestBodySize = maxRequestBodySize;
        TailHandler tail = new TailHandler();

        // request chain: Head -> RequestParserHandler -> UriParserHandler -> OperationResolverHandler -> Tail
        requestChain = new HeadHandler(
                new RequestParserHandler(
                        new UriParserHandler(
                                new OperationResolverHandler(tail)),
                        maxRequestBodySize));

        // body chain: Head -> BodyParserHandler -> Tail
        bodyChain = new HeadHandler(new BodyParserHandler(tail));

        // parser selection chain: Head -> ParserSelectionHandler -> Tail
        parserSelectionChain = new HeadHandler(new ParserSelectionHandler(tail));

        // serializer selection chain: Head -> SerializerSelectionHandler -> Tail
        serializerSelectionChain = new HeadHandler(new SerializerSelectionHandler(tail));

        // CRUD chains with permission checks
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

        // response chain: Head -> SpeedyResponseWriterHandler -> Tail
        responseChain = new HeadHandler(new SpeedyResponseWriterHandler(tail));
    }

    @Override
    public QueryProcessor prepare(RequestContext ctx) throws SpeedyHttpException {
        ISpeedyConfiguration config = ctx.getConfiguration();
        SpeedyDialect dialect = ctx.getDialect();
        DataSource dataSource = config.dataSourcePerReq();
        QueryProcessor qp = queryProcessorCache.computeIfAbsent(
                dataSource, ds -> new JooqQueryProcessorImpl(ds, dialect));
        return qp;
    }

    @Override
    public SpeedyRequest parseRequest(RequestContext ctx) throws SpeedyHttpException {
        requestChain.process(ctx);
        return ctx.getRequest();
    }

    @Override
    public SpeedyBody parseBody(RequestContext ctx) throws SpeedyHttpException {
        bodyChain.process(ctx);
        return ctx.getRequest().getBody();
    }

    @Override
    public void selectBodyParser(RequestContext ctx) throws SpeedyHttpException {
        parserSelectionChain.process(ctx);
    }

    @Override
    public void selectSerializer(RequestContext ctx) throws SpeedyHttpException {
        serializerSelectionChain.process(ctx);
    }

    @Override
    public SpeedyResponse get(RequestContext ctx) throws SpeedyHttpException {
        getChain.process(ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse query(RequestContext ctx) throws SpeedyHttpException {
        queryChain.process(ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse create(RequestContext ctx) throws SpeedyHttpException {
        createChain.process(ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse update(RequestContext ctx) throws SpeedyHttpException {
        updateChain.process(ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public SpeedyResponse delete(RequestContext ctx) throws SpeedyHttpException {
        deleteChain.process(ctx);
        return ctx.getSpeedyResponse();
    }

    @Override
    public void writeResponse(RequestContext ctx) throws SpeedyHttpException {
        responseChain.process(ctx);
    }
}
