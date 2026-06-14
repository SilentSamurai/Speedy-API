package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.engine.ContentNegotiationManager;
import com.github.silent.samurai.speedy.engine.SpeedyEngine;
import com.github.silent.samurai.speedy.engine.SpeedyEngineImpl;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.RegistryImpl;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.conversion.ext.SpeedyTypeModule;
import com.github.silent.samurai.speedy.conversion.registry.JavaTypeRegistry;
import com.github.silent.samurai.speedy.conversion.walker.java.JavaToSpeedy;
import com.github.silent.samurai.speedy.conversion.walker.java.SpeedyToJava;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import com.github.silent.samurai.speedy.models.SpeedyErrorResponse;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.utils.AdviceExceptionMapper;
import com.github.silent.samurai.speedy.utils.DefaultExceptionMapper;
import com.github.silent.samurai.speedy.validation.MetaModelVerifier;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.StreamSupport;

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
    private final ContentNegotiationManager contentNegotiationManager;

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

        /// Build the conversion context with built-in defaults, then let SPI
        /// modules (e.g. speedy-json-io's JsonSpeedyProvider) contribute their
        /// registries, and finally apply user-supplied type modules so custom
        /// encodings can override or extend the built-in ones.
        this.conversionContext = ConversionContext.withDefaults();
        List<ISpeedyIoProvider> providers = StreamSupport
                .stream(ServiceLoader.load(ISpeedyIoProvider.class).spliterator(), false)
                .toList();
        for (ISpeedyIoProvider provider : providers) {
            provider.contributeModule(conversionContext);
        }
        for (SpeedyTypeModule module : speedyConfiguration.typeModules()) {
            module.contribute(conversionContext);
        }

        log.info("Loaded {} IO provider(s): {}", providers.size(),
                providers.stream().map(ISpeedyIoProvider::getContentType).toList());

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

        /// Discover all I/O format providers once at startup via SPI, validate that
        /// no two providers claim the same content type, and build content-type lookup
        /// maps. The maps are injected into the engine so handlers only need to select.
        Map<String, ISpeedyIoProvider> providerMap = buildProviderMap(
                providers, ISpeedyIoProvider::getContentType, "ISpeedyIoProvider");

        this.contentNegotiationManager = new ContentNegotiationManager(providerMap);

        this.engine = new SpeedyEngineImpl(configuration, dialect, metaModel, eventProcessor, validationProcessor,
                maxRequestBodySize, conversionContext, contentNegotiationManager);
    }

    static <T> Map<String, T> buildProviderMap(List<T> providers,
                                               Function<T, String> contentTypeFn,
                                               String providerTypeName) throws InternalServerError {
        Map<String, T> map = new HashMap<>();
        for (T provider : providers) {
            String contentType = contentTypeFn.apply(provider).toLowerCase();
            if (map.containsKey(contentType)) {
                throw new InternalServerError(
                        "Duplicate " + providerTypeName + " content type '" + contentType + "': "
                                + map.get(contentType).getClass().getName() + " and " + provider.getClass().getName()
                );
            }
            map.put(contentType, provider);
        }
        return map;
    }

    public void processReqV2(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Start with the baseline JSON serializer as a fallback in case negotiation itself fails.
        IResponseSerializerV2 serializer = contentNegotiationManager.createDefaultSerializer();
        try {
            // 1. Get the request. prepare() stores the QueryProcessor in ctx.
            SpeedyContext ctx = engine.newContext(request, response);
            engine.prepare(ctx);

            // 2-3. Parse the URI and the headers, then resolve which operation this is.
            SpeedyUriContext uriContext = engine.parseUri(ctx);
            SpeedyHeaders headers       = engine.parseHeaders(ctx);
            SpeedyRequestType type      = engine.resolveOperation(ctx);

            // 4. Negotiate an output + input format from the parsed headers. From here on, errors
            // in the catch block render in the client's negotiated format (before this, the
            // baseline JSON fallback is used).
            serializer                  = engine.selectSerializer(ctx);
            IRequestBodyParser parser   = engine.selectBodyParser(ctx);

            // 5. Single dispatch on the request type — this is the ONLY switch, and it lives
            // HERE, not inside a handler. Write ops parse their body (with the parser selected
            // above), then run the operation; read ops (GET_LIST, METADATA) carry nobody.
            // See SpeedyEngine Javadoc.
            SpeedyResponse resp = switch (type) {
                case GET_LIST -> engine.get(ctx);
                case QUERY    -> { engine.parseQueryBody(ctx);  yield engine.query(ctx); }
                case CREATE   -> { engine.parseCreateBody(ctx); yield engine.create(ctx); }
                case UPDATE   -> { engine.parseUpdateBody(ctx); yield engine.update(ctx); }
                case DELETE   -> { engine.parseDeleteBody(ctx); yield engine.delete(ctx); }
                case METADATA -> engine.metadata(ctx);
            };

            // 6. Write the response.
            serializer.write(resp, response);
        } catch (Throwable e) {
            if (e instanceof Error) throw (Error) e;
            int status = exceptionMapper.getStatus(e);
            String message = exceptionMapper.getMessage(e);
            try {
                serializer.write(
                        SpeedyErrorResponse.builder().status(status).message(message).build(),
                        response);
            } catch (SpeedyHttpException writeFailure) {
                log.error("Failed to write error response for {}", request.getRequestURI(), writeFailure);
            }
            log.error("Exception {} ", request.getRequestURI(), e);
        } finally {
            response.getWriter().flush();
        }
    }

}
