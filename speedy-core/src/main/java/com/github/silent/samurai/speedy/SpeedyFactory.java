package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.RegistryImpl;
import com.github.silent.samurai.speedy.events.VirtualEntityProcessor;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.impl.query.QueryProcessorImpl;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.responses.MultiPayloadWrapper;
import com.github.silent.samurai.speedy.responses.SinglePayloadWrapper;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.request.delete.DeleteDataHandler;
import com.github.silent.samurai.speedy.request.delete.DeleteRequestContext;
import com.github.silent.samurai.speedy.request.delete.DeleteRequestParser;
import com.github.silent.samurai.speedy.request.get.GetDataHandler;
import com.github.silent.samurai.speedy.request.get.GetRequestContext;
import com.github.silent.samurai.speedy.request.get.GetRequestParser;
import com.github.silent.samurai.speedy.request.post.PostDataHandler;
import com.github.silent.samurai.speedy.request.post.PostRequestContext;
import com.github.silent.samurai.speedy.request.post.PostRequestParser;
import com.github.silent.samurai.speedy.request.put.PutRequestContext;
import com.github.silent.samurai.speedy.request.put.PutRequestParser;
import com.github.silent.samurai.speedy.request.put.UpdateDataHandler;
import com.github.silent.samurai.speedy.serializers.JSONSerializer;
import com.github.silent.samurai.speedy.utils.ExceptionUtils;
import com.github.silent.samurai.speedy.validation.MetaModelVerifier;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import lombok.Getter;
import org.jooq.SQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class SpeedyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyFactory.class);

    private final ISpeedyConfiguration speedyConfiguration;
    private final MetaModelProcessor metaModelProcessor;
    private final ValidationProcessor validationProcessor;
    private final EventProcessor eventProcessor;
    private final RegistryImpl eventRegistry;
    private final VirtualEntityProcessor vEntityProcessor;

    public SpeedyFactory(ISpeedyConfiguration speedyConfiguration) {
        this.speedyConfiguration = speedyConfiguration;
        this.metaModelProcessor = speedyConfiguration.createMetaModelProcessor();
        new MetaModelVerifier(metaModelProcessor).verify();
        this.validationProcessor = new ValidationProcessor(speedyConfiguration.getCustomValidator(), metaModelProcessor);
        this.validationProcessor.process();
        // events
        this.eventRegistry = new RegistryImpl();
        speedyConfiguration.register(eventRegistry);
        this.eventProcessor = new EventProcessor(metaModelProcessor, eventRegistry);
        this.eventProcessor.processRegistry();
        this.vEntityProcessor = new VirtualEntityProcessor(metaModelProcessor, eventRegistry);
        this.vEntityProcessor.processRegistry();
    }

    public void processGETRequests(HttpServletRequest request, HttpServletResponse response, QueryProcessor queryProcessor)
            throws Exception {
        GetRequestContext context = new GetRequestContext(request, response, metaModelProcessor);
        new GetRequestParser(context).process();
        Optional<List<SpeedyEntity>> requestedData = new GetDataHandler(context).processMany(queryProcessor);
        if (requestedData.isEmpty()) {
            throw new NotFoundException();
        }
        List<SpeedyEntity> speedyEntities = requestedData.get();
        IResponseSerializer jsonSerializer = new JSONSerializer(context);
        jsonSerializer.write(speedyEntities);
    }

    public void processPOSTRequests(HttpServletRequest request, HttpServletResponse response, QueryProcessor queryProcessor) throws Exception {
        PostRequestContext context = new PostRequestContext(
                request,
                response,
                metaModelProcessor,
                validationProcessor,
                eventProcessor,
                vEntityProcessor,
                queryProcessor);
        new PostRequestParser(context).processBatch();
        Optional<List<SpeedyEntity>> savedEntities = new PostDataHandler(context).processBatch();
        IResponseSerializer jsonSerializer = new JSONSerializer(context, KeyFieldMetadata.class::isInstance);
        jsonSerializer.write(savedEntities.orElse(Collections.emptyList()));
    }

    public void processPUTRequests(HttpServletRequest request, HttpServletResponse response, QueryProcessor queryProcessor) throws Exception {
        PutRequestContext context = new PutRequestContext(
                request,
                response,
                metaModelProcessor,
                queryProcessor,
                validationProcessor,
                eventProcessor,
                vEntityProcessor);
        new PutRequestParser(context).process();
        Optional<SpeedyEntity> savedEntity = new UpdateDataHandler(context).process();
        if (savedEntity.isEmpty()) {
            throw new NotFoundException();
        }
        IResponseSerializer jsonSerializer = new JSONSerializer(context);
        SpeedyEntity speedyEntity = savedEntity.get();
        jsonSerializer.write(speedyEntity);
    }

    public void processDELETERequests(HttpServletRequest request, HttpServletResponse response, QueryProcessor queryProcessor) throws Exception {
        DeleteRequestContext context = new DeleteRequestContext(
                request, response,
                metaModelProcessor,
                validationProcessor,
                queryProcessor,
                eventProcessor,
                vEntityProcessor);
        new DeleteRequestParser(context).process();
        Optional<List<SpeedyEntity>> removedEntities = new DeleteDataHandler(context).process();
        IResponseSerializer jsonSerializer = new JSONSerializer(context, KeyFieldMetadata.class::isInstance);
        List<SpeedyEntity> speedyEntities = removedEntities.get();
        jsonSerializer.write(speedyEntities);
    }

    public void requestResource(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LOGGER.info("REQ: {} {} ", request.getMethod(), request.getRequestURI());

        DataSource dataSource = speedyConfiguration.getDataSource();
        String dialect = speedyConfiguration.getDialect();
        QueryProcessor queryProcessor = new QueryProcessorImpl(dataSource, SQLDialect.valueOf(dialect));

        try {
            if (request.getMethod().equals(HttpMethod.GET.name())) {
                processGETRequests(request, response, queryProcessor);
            } else if (request.getMethod().equals(HttpMethod.POST.name())) {
                processPOSTRequests(request, response, queryProcessor);
            } else if (request.getMethod().equals(HttpMethod.PUT.name())) {
                processPUTRequests(request, response, queryProcessor);
            } else if (request.getMethod().equals(HttpMethod.DELETE.name())) {
                processDELETERequests(request, response, queryProcessor);
            } else {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (SpeedyHttpException e) {
            ExceptionUtils.writeException(response, e);
            LOGGER.error("Exception at get {} ", request.getRequestURI(), e);
        } catch (Exception e) {
            response.setStatus(ExceptionUtils.getStatusFromException(e));
            LOGGER.error("Exception at get {} ", request.getRequestURI(), e);
        } catch (Throwable e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            metaModelProcessor.closeQueryProcessor(queryProcessor);
            response.getWriter().flush();
        }
    }


}
