package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.events.RegistryImpl;
import com.github.silent.samurai.speedy.events.VirtualEntityProcessor;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.models.PayloadWrapper;
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
import com.github.silent.samurai.speedy.serializers.json.JSONSerializer;
import com.github.silent.samurai.speedy.utils.ExceptionUtils;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    public void processGETRequests(HttpServletRequest request, HttpServletResponse response, EntityManager entityManager)
            throws Exception {
        GetRequestContext context = new GetRequestContext(request, response, metaModelProcessor, entityManager);
        new GetRequestParser(context).process();
        Optional<Object> requestData = new GetDataHandler(context).process();
        if (requestData.isEmpty()) {
            throw new NotFoundException();
        }
        IResponseSerializer jsonSerializer = new JSONSerializer(context);
        PayloadWrapper responseWrapper = PayloadWrapper.wrapperInResponse(requestData.get());
        int pageNumber = context.getParser().getQueryOrDefault("pageNo", Integer.class, 0);
        responseWrapper.setPageIndex(pageNumber);
        response.setContentType(jsonSerializer.getContentType());
        response.setStatus(HttpServletResponse.SC_OK);
        jsonSerializer.writeResponse(responseWrapper);
    }

    public void processPOSTRequests(HttpServletRequest request, HttpServletResponse response, EntityManager entityManager) throws Exception {
        PostRequestContext context = new PostRequestContext(
                request,
                response,
                metaModelProcessor,
                entityManager,
                validationProcessor,
                eventProcessor,
                vEntityProcessor);
        new PostRequestParser(context).processBatch();
        Optional<List<Object>> savedEntities = new PostDataHandler(context).processBatch();
        IResponseSerializer jsonSerializer = new JSONSerializer(context, KeyFieldMetadata.class::isInstance);
        PayloadWrapper responseWrapper = PayloadWrapper.wrapperInResponse(savedEntities.orElse(Collections.emptyList()));
        response.setContentType(jsonSerializer.getContentType());
        response.setStatus(HttpServletResponse.SC_OK);
        jsonSerializer.writeResponse(responseWrapper);
    }

    public void processPUTRequests(HttpServletRequest request, HttpServletResponse response, EntityManager entityManager) throws Exception {
        PutRequestContext context = new PutRequestContext(
                request,
                response,
                metaModelProcessor,
                entityManager,
                validationProcessor,
                eventProcessor, vEntityProcessor);
        new PutRequestParser(context).process();
        Optional<Object> savedEntity = new UpdateDataHandler(context).process();
        if (savedEntity.isEmpty()) {
            throw new NotFoundException();
        }
        IResponseSerializer jsonSerializer = new JSONSerializer(context);
        PayloadWrapper responseWrapper = PayloadWrapper.wrapperInResponse(savedEntity.get());
        response.setContentType(jsonSerializer.getContentType());
        response.setStatus(HttpServletResponse.SC_OK);
        jsonSerializer.writeResponse(responseWrapper);
    }

    public void processDELETERequests(HttpServletRequest request, HttpServletResponse response, EntityManager entityManager) throws Exception {
        DeleteRequestContext context = new DeleteRequestContext(request, response, metaModelProcessor, validationProcessor, entityManager, eventProcessor, vEntityProcessor);
        new DeleteRequestParser(context).process();
        Optional<List<Object>> removedEntities = new DeleteDataHandler(context).process();
        IResponseSerializer jsonSerializer = new JSONSerializer(context, KeyFieldMetadata.class::isInstance);
        PayloadWrapper responseWrapper = PayloadWrapper.wrapperInResponse(removedEntities.orElse(Collections.emptyList()));
        response.setContentType(jsonSerializer.getContentType());
        response.setStatus(HttpServletResponse.SC_OK);
        jsonSerializer.writeResponse(responseWrapper);
    }

    public void requestResource(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManager entityManager = null;
        LOGGER.info("REQ: {} {} ", request.getMethod(), request.getRequestURI());
        try {
            entityManager = speedyConfiguration.createEntityManager();
            if (request.getMethod().equals(HttpMethod.GET.name())) {
                processGETRequests(request, response, entityManager);
            } else if (request.getMethod().equals(HttpMethod.POST.name())) {
                processPOSTRequests(request, response, entityManager);
            } else if (request.getMethod().equals(HttpMethod.PUT.name())) {
                processPUTRequests(request, response, entityManager);
            } else if (request.getMethod().equals(HttpMethod.DELETE.name())) {
                processDELETERequests(request, response, entityManager);
            } else {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (SpeedyHttpException e) {
            ExceptionUtils.writeException(response, e);
            LOGGER.error("Exception at get {} ", request.getRequestURI(), e);
        } catch (Exception e) {
            response.setStatus(ExceptionUtils.getStatusFromException(e));
//            response.getWriter().write(e.getMessage());
            LOGGER.error("Exception at get {} ", request.getRequestURI(), e);
        } catch (Throwable e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            response.getWriter().write(e.getMessage());
        } finally {
            if (entityManager != null)
                entityManager.close();
            response.getWriter().flush();
        }
    }


}
