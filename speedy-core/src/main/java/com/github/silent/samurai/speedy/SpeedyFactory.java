package com.github.silent.samurai.speedy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.RegistryImpl;
import com.github.silent.samurai.speedy.events.VirtualEntityProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.query.Json2SpeedyQueryBuilder;
import com.github.silent.samurai.speedy.query.jooq.JooqQueryProcessorImpl;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.request.IResponseContext;
import com.github.silent.samurai.speedy.request.DeleteDataHandler;
import com.github.silent.samurai.speedy.request.GetDataHandler;
import com.github.silent.samurai.speedy.request.IRequestContextImpl;
import com.github.silent.samurai.speedy.request.PostDataHandler;
import com.github.silent.samurai.speedy.request.UpdateDataHandler;
import com.github.silent.samurai.speedy.serializers.JSONSerializer;
import com.github.silent.samurai.speedy.utils.CommonUtil;
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
    private final QueryProcessor queryProcessor;


    public SpeedyFactory(ISpeedyConfiguration speedyConfiguration) {
        this.speedyConfiguration = speedyConfiguration;
        this.metaModelProcessor = speedyConfiguration.createMetaModelProcessor();
        new MetaModelVerifier(metaModelProcessor).verify();

        // events
        this.eventRegistry = new RegistryImpl();
        speedyConfiguration.register(eventRegistry);
        this.eventProcessor = new EventProcessor(metaModelProcessor, eventRegistry);
        this.eventProcessor.processRegistry();

        this.vEntityProcessor = new VirtualEntityProcessor(metaModelProcessor, eventRegistry);
        this.vEntityProcessor.processRegistry();

        this.validationProcessor = new ValidationProcessor(eventRegistry.getValidators(), metaModelProcessor);
        this.validationProcessor.process();

        DataSource dataSource = speedyConfiguration.getDataSource();
        String dialect = speedyConfiguration.getDialect();
        this.queryProcessor = new JooqQueryProcessorImpl(dataSource, SQLDialect.valueOf(dialect));
    }

    public void processGetRequests(IRequestContextImpl context, SpeedyQuery speedyQuery) throws Exception {
        Optional<List<SpeedyEntity>> requestedData = new GetDataHandler(context).processMany(speedyQuery);
        if (requestedData.isEmpty()) {
            throw new NotFoundException();
        }
        List<SpeedyEntity> speedyEntities = requestedData.get();
        IResponseContext responseContext = context.createResponseContext().build();
        IResponseSerializer jsonSerializer = new JSONSerializer(responseContext);
        jsonSerializer.write(speedyEntities);
    }

    public void processCreateRequests(IRequestContextImpl context) throws Exception {
        EntityMetadata resourceMetadata = context.getEntityMetadata();
        ObjectMapper json = CommonUtil.json();
        JsonNode jsonBody = json.readTree(context.getRequest().getReader());
        Optional<List<SpeedyEntity>> savedEntities = new PostDataHandler(context).process(resourceMetadata, jsonBody);
        IResponseContext responseContext = context.createResponseContext().build();
        IResponseSerializer jsonSerializer = new JSONSerializer(responseContext, KeyFieldMetadata.class::isInstance);
        jsonSerializer.write(savedEntities.orElse(Collections.emptyList()));

    }

    public void processQueryRequest(IRequestContextImpl context) throws Exception {
        EntityMetadata resourceMetadata = context.getEntityMetadata();
        ObjectMapper json = CommonUtil.json();
        JsonNode jsonBody = json.readTree(context.getRequest().getReader());
        Json2SpeedyQueryBuilder json2SpeedyQueryBuilder = new Json2SpeedyQueryBuilder(metaModelProcessor, resourceMetadata, jsonBody);
        SpeedyQuery speedyQuery = json2SpeedyQueryBuilder.build();
        List<SpeedyEntity> speedyEntities = queryProcessor.executeMany(speedyQuery);
        IResponseContext responseContext = context.createResponseContext().pageNo(speedyQuery.getPageInfo().getPageNo()).expands(speedyQuery.getExpand()).build();
        IResponseSerializer jsonSerializer = new JSONSerializer(responseContext);
        jsonSerializer.write(speedyEntities);
    }

    public void processPutRequests(IRequestContextImpl context) throws Exception {
        Optional<SpeedyEntity> savedEntity = new UpdateDataHandler(context).process();
        if (savedEntity.isEmpty()) {
            throw new NotFoundException();
        }
        IResponseContext responseContext = context.createResponseContext().build();
        IResponseSerializer jsonSerializer = new JSONSerializer(responseContext);
        SpeedyEntity speedyEntity = savedEntity.get();
        jsonSerializer.write(speedyEntity);
    }

    public void processDeleteRequests(IRequestContextImpl context) throws Exception {

        Optional<List<SpeedyEntity>> removedEntities = new DeleteDataHandler(context).process();
        IResponseContext responseContext = context.createResponseContext().build();

        IResponseSerializer jsonSerializer = new JSONSerializer(responseContext, KeyFieldMetadata.class::isInstance);
        List<SpeedyEntity> speedyEntities = removedEntities.get();

        jsonSerializer.write(speedyEntities);
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String requestURI = CommonUtil.getRequestURI(request);
        String method = request.getMethod();

        try {

            SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, requestURI);
            SpeedyQuery uriSpeedyQuery = parser.parse();

            EntityMetadata resourceMetadata = uriSpeedyQuery.getFrom();

            IRequestContextImpl context = new IRequestContextImpl(request, response, metaModelProcessor, validationProcessor, eventProcessor, vEntityProcessor, queryProcessor, resourceMetadata);


            if (method.equals(HttpMethod.GET.name())) {
                processGetRequests(context, uriSpeedyQuery);
            } else if (method.equals(HttpMethod.POST.name())) {

                if (requestURI.contains("$query")) {
                    processQueryRequest(context);
                } else if (requestURI.contains("$create")) {
                    processCreateRequests(context);
                } else {
                    throw new BadRequestException("not a valid request");
                }

            } else if (method.equals(HttpMethod.PUT.name()) || method.equals(HttpMethod.PATCH.name())) {

                if (requestURI.contains("$update")) {
                    processPutRequests(context);
                } else {
                    throw new BadRequestException("not a valid request");
                }

            } else if (method.equals(HttpMethod.DELETE.name())) {

                if (requestURI.contains("$delete")) {
                    processDeleteRequests(context);
                } else {
                    throw new BadRequestException("not a valid request");
                }

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
            response.getWriter().flush();
        }
    }


}
