package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.engine.SpeedyEngine;
import com.github.silent.samurai.speedy.engine.SpeedyEngineImpl;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.RegistryImpl;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import com.github.silent.samurai.speedy.request.SpeedyRequest;
import com.github.silent.samurai.speedy.utils.AdviceExceptionMapper;
import com.github.silent.samurai.speedy.utils.DefaultExceptionMapper;
import com.github.silent.samurai.speedy.utils.ExceptionUtils;
import com.github.silent.samurai.speedy.validation.MetaModelVerifier;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Getter
@Slf4j
public class SpeedyFactory {

    private final ISpeedyConfiguration speedyConfiguration;
    private final MetaModel metaModel;
    private final ValidationProcessor validationProcessor;
    private final EventProcessor eventProcessor;
    private final RegistryImpl eventRegistry;
    private final ISpeedyExceptionMapper exceptionMapper;
    private final SpeedyDialect dialect;
    private final ISpeedyConfiguration configuration;
    private final long maxRequestBodySize;
    private final SpeedyEngine engine;

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

        this.eventRegistry = new RegistryImpl();
        speedyConfiguration.register(eventRegistry);
        this.eventProcessor = new EventProcessor(metaModel, eventRegistry);
        this.eventProcessor.processRegistry();

        this.exceptionMapper = new DefaultExceptionMapper(new AdviceExceptionMapper(eventRegistry.getControllerAdvices()));

        this.validationProcessor = new ValidationProcessor(eventRegistry.getValidators(), metaModel);
        this.validationProcessor.process();

        configuration = speedyConfiguration;
        dialect = speedyConfiguration.getDialect();

        this.engine = new SpeedyEngineImpl(configuration, dialect, metaModel, eventProcessor, validationProcessor, maxRequestBodySize);
    }

    public void processReqV2(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            QueryProcessor qp = engine.prepare();

            SpeedyRequest req = engine.parseRequest(request);

            IRequestBodyParser parser = engine.selectBodyParser(req);
            SpeedyBody body = engine.parseBody(parser, req, qp);

            SpeedyResponse resp = switch (req.getRequestType()) {
                case GET_LIST -> engine.get(req, body, qp);
                case QUERY -> engine.query(req, body, qp);
                case CREATE -> engine.create(req, body, qp);
                case UPDATE -> engine.update(req, body, qp);
                case DELETE -> engine.delete(req, body, qp);
            };

            IResponseSerializerV2 serializer = engine.selectSerializer(request, req);
            engine.writeResponse(serializer, resp, response);

        } catch (Throwable e) {
            if (e instanceof Error) throw (Error) e;
            int status = exceptionMapper.getStatus(e);
            String message = exceptionMapper.getMessage(e);
            ExceptionUtils.writeException(response, status, message);
            log.error("Exception {} ", request.getRequestURI(), e);
        } finally {
            response.getWriter().flush();
        }
    }

}
