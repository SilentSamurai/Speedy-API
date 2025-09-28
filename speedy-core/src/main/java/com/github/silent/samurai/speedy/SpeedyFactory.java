package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.RegistryImpl;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.handlers.*;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import com.github.silent.samurai.speedy.query.JooqQueryProcessorImpl;
import com.github.silent.samurai.speedy.request.RequestContext;
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

@Getter
public class SpeedyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyFactory.class);

    private final ISpeedyConfiguration speedyConfiguration;
    private final MetaModel metaModel;
    private final ValidationProcessor validationProcessor;
    private final EventProcessor eventProcessor;
    private final RegistryImpl eventRegistry;
    //    private final QueryProcessor queryProcessor;
    private final SpeedyDialect dialect;
    private final ISpeedyConfiguration configuration;
    Handler chain = createHandlerChain();


    public SpeedyFactory(ISpeedyConfiguration speedyConfiguration) throws SpeedyHttpException {
        this.speedyConfiguration = speedyConfiguration;

        MetaModelProcessor metaModelProcessor = speedyConfiguration.metaModelProcessor();
        metaModelProcessor.processMetaModel(MetadataBuilder.builder());
        this.metaModel = metaModelProcessor.getMetaModel();

        new MetaModelVerifier(metaModel).verify();

        // events
        this.eventRegistry = new RegistryImpl();
        speedyConfiguration.register(eventRegistry);
        this.eventProcessor = new EventProcessor(metaModel, eventRegistry);
        this.eventProcessor.processRegistry();

        this.validationProcessor = new ValidationProcessor(eventRegistry.getValidators(), metaModel);
        this.validationProcessor.process();

        configuration = speedyConfiguration;
        dialect = speedyConfiguration.getDialect();
    }

    private QueryProcessor createQueryProcessor() {
        DataSource dataSource = speedyConfiguration.dataSourcePerReq();
        return new JooqQueryProcessorImpl(dataSource, dialect);
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
            this.chain.process(requestContext);
        } catch (SpeedyHttpException e) {
            ExceptionUtils.writeException(response, e);
            LOGGER.error("Exception {} ", request.getRequestURI(), e);
        } catch (Exception e) {
            response.setStatus(ExceptionUtils.getStatusFromException(e));
            LOGGER.error("Exception {} ", request.getRequestURI(), e);
        } catch (Throwable e) {
            LOGGER.error("Exception {} ", request.getRequestURI(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            response.getWriter().flush();
        }
    }

    private Handler createHandlerChain() {
        Handler tail = new TailHandler();

        Handler rw = new SpeedyResponseWriterHandler(tail);

        // switch

        Handler gh = new GetHandler(rw);
        Handler qh = new QueryHandler(rw);

        Handler ch = new CreateHandler(rw);
        Handler uh = new UpdateHandler(rw);
        Handler dh = new DeleteHandler(rw);

        // switch
        Handler sh = new SwitchHandler(gh, qh, ch, uh, dh);

        Handler queryProcessorInit = new CreateQueryProcessorHandler(sh);
        Handler ech = new EntityCaptureHandler(queryProcessorInit);
        Handler requestParserHandler = new RequestParserHandler(ech);
        return new HeadHandler(requestParserHandler);
    }


}
