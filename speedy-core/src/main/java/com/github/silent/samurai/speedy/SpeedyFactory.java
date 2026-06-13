package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.engine.SpeedyEngine;
import com.github.silent.samurai.speedy.engine.SpeedyEngineImpl;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.RegistryImpl;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.conversion.ext.SpeedyTypeModule;
import com.github.silent.samurai.speedy.conversion.registry.JavaTypeRegistry;
import com.github.silent.samurai.speedy.conversion.walker.java.JavaToSpeedy;
import com.github.silent.samurai.speedy.conversion.walker.java.SpeedyToJava;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import com.github.silent.samurai.speedy.request.RequestContext;
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
    private final ConversionContext conversionContext;

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

        this.exceptionMapper = new DefaultExceptionMapper(new AdviceExceptionMapper(eventRegistry.getControllerAdvices()));

        configuration = speedyConfiguration;
        dialect = speedyConfiguration.getDialect();

        /// Build the conversion context with built-in defaults, then apply any
        /// user-supplied type modules so that custom encodings/decodings take effect.
        this.conversionContext = ConversionContext.withDefaults();
        for (SpeedyTypeModule module : speedyConfiguration.typeModules()) {
            module.contribute(conversionContext);
        }

        /// Extract the Java-type registry from the context and create the
        /// serializer / deserializer pair that the event and validation processors
        /// will use for all Java ↔ SpeedyValue conversions.
        JavaTypeRegistry jtr = conversionContext.get(JavaTypeRegistry.class);
        SpeedyToJava serializer = new SpeedyToJava(jtr);
        JavaToSpeedy deserializer = new JavaToSpeedy(jtr);

        this.eventProcessor = new EventProcessor(metaModel, eventRegistry, serializer, deserializer);
        this.eventProcessor.processRegistry();

        this.validationProcessor = new ValidationProcessor(eventRegistry.getValidators(), metaModel, serializer, deserializer);
        this.validationProcessor.process();

        this.engine = new SpeedyEngineImpl(configuration, dialect, metaModel, eventProcessor, validationProcessor, maxRequestBodySize, conversionContext);
    }

    public void processReqV2(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            RequestContext ctx = engine.newContext(request, response);
            ctx.put(QueryProcessor.class, engine.prepare());
            engine.parseRequest(ctx);
            engine.selectBodyParser(ctx);
            engine.parseBody(ctx);
            engine.execute(ctx);
            engine.selectSerializer(ctx);
            engine.writeResponse(ctx);
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
