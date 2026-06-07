package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.RegistryImpl;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.handlers.*;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.ISpeedyExceptionMapper;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import com.github.silent.samurai.speedy.query.JooqQueryProcessorImpl;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.utils.AdviceExceptionMapper;
import com.github.silent.samurai.speedy.utils.DefaultExceptionMapper;
import com.github.silent.samurai.speedy.utils.ExceptionUtils;
import com.github.silent.samurai.speedy.validation.MetaModelVerifier;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class SpeedyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyFactory.class);

    private final ISpeedyConfiguration speedyConfiguration;
    private final MetaModel metaModel;
    private final ValidationProcessor validationProcessor;
    private final EventProcessor eventProcessor;
    private final RegistryImpl eventRegistry;
    private final ISpeedyExceptionMapper exceptionMapper;
    private final SpeedyDialect dialect;
    private final ISpeedyConfiguration configuration;
    private final long maxRequestBodySize;
    private final ConcurrentHashMap<DataSource, QueryProcessor> queryProcessorCache = new ConcurrentHashMap<>();
    Handler chain;


    public SpeedyFactory(ISpeedyConfiguration speedyConfiguration) throws SpeedyHttpException {
        this(speedyConfiguration, speedyConfiguration.getMaxRequestBodySize());
    }

    public SpeedyFactory(ISpeedyConfiguration speedyConfiguration, long maxRequestBodySize) throws SpeedyHttpException {
        this.speedyConfiguration = speedyConfiguration;
        this.maxRequestBodySize = maxRequestBodySize;

        MetaModelProcessor metaModelProcessor = speedyConfiguration.metaModelProcessor();
        metaModelProcessor.processMetaModel(MetadataBuilder.builder());
        this.metaModel = metaModelProcessor.getMetaModel();

        new MetaModelVerifier(metaModel).verify();

        // events
        this.eventRegistry = new RegistryImpl();
        speedyConfiguration.register(eventRegistry);
        this.eventProcessor = new EventProcessor(metaModel, eventRegistry);
        this.eventProcessor.processRegistry();

        this.exceptionMapper = new DefaultExceptionMapper(
                new AdviceExceptionMapper(eventRegistry.getControllerAdvices()));

        this.validationProcessor = new ValidationProcessor(eventRegistry.getValidators(), metaModel);
        this.validationProcessor.process();

        configuration = speedyConfiguration;
        dialect = speedyConfiguration.getDialect();

        this.chain = createHandlerChain();
    }

    public void processReqV2(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RequestContext requestContext = new RequestContext(
                configuration,
                dialect,
                metaModel,
                request,
                response,
                eventProcessor,
                validationProcessor
        );
        try {
            DataSource dataSource = configuration.dataSourcePerReq();
            QueryProcessor queryProcessor = queryProcessorCache.computeIfAbsent(
                    dataSource, ds -> new JooqQueryProcessorImpl(ds, dialect));
            requestContext.setQueryProcessor(queryProcessor);

            this.chain.process(requestContext);
        } catch (Throwable e) {
            if (e instanceof Error) throw (Error) e;
            int status = exceptionMapper.getStatus(e);
            String message = exceptionMapper.getMessage(e);
            ExceptionUtils.writeException(response, status, message);
            LOGGER.error("Exception {} ", request.getRequestURI(), e);
        } finally {
            response.getWriter().flush();
        }
    }

    private Handler createHandlerChain() {
        Handler tail = new TailHandler();

        Handler rw = new SpeedyResponseWriterHandler(tail);
        Handler ss = new SerializerSelectionHandler(rw);

        // switch

        Handler gh = new GetHandler(ss);
        Handler qh = new QueryHandler(ss);

        Handler ch = new CreateHandler(ss);
        Handler uh = new UpdateHandler(ss);
        Handler dh = new DeleteHandler(ss);

        // switch
        Handler sh = new SwitchHandler(gh, qh, ch, uh, dh);

        Handler ech = new EntityCaptureHandler(sh);
        Handler requestParserHandler = new RequestParserHandler(ech, maxRequestBodySize);
        return new HeadHandler(requestParserHandler);
    }


}
