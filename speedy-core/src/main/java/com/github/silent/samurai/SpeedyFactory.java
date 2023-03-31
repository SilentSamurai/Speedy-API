package com.github.silent.samurai;

import com.github.silent.samurai.exceptions.ResourceNotFoundException;
import com.github.silent.samurai.factory.AbstractFactory;
import com.github.silent.samurai.interfaces.IResponseSerializer;
import com.github.silent.samurai.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.models.BaseResponsePayloadImpl;
import com.github.silent.samurai.request.delete.DeleteDataHandler;
import com.github.silent.samurai.request.delete.DeleteRequestContext;
import com.github.silent.samurai.request.delete.DeleteRequestParser;
import com.github.silent.samurai.request.get.GetDataHandler;
import com.github.silent.samurai.request.get.GetRequestContext;
import com.github.silent.samurai.request.get.GetRequestParser;
import com.github.silent.samurai.request.post.CreateDataHandler;
import com.github.silent.samurai.request.post.PostRequestContext;
import com.github.silent.samurai.request.post.PostRequestParser;
import com.github.silent.samurai.request.put.PutRequestContext;
import com.github.silent.samurai.request.put.PutRequestParser;
import com.github.silent.samurai.request.put.UpdateDataHandler;
import com.github.silent.samurai.utils.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpMethod;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class SpeedyFactory {

    Logger logger = LogManager.getLogger(SpeedyFactory.class);

    private final ISpeedyConfiguration speedyConfiguration;
    private final MetaModelProcessor metaModelProcessor;


    public SpeedyFactory(ISpeedyConfiguration speedyConfiguration) {
        this.speedyConfiguration = speedyConfiguration;
        this.metaModelProcessor = speedyConfiguration.createMetaModelProcessor();
    }

    public void processGETRequests(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        EntityManager entityManager = null;
        try {
            entityManager = speedyConfiguration.createEntityManager();
            GetRequestContext context = new GetRequestContext(request, metaModelProcessor, entityManager);
            new GetRequestParser(context).process();
            Optional<Object> requestData = new GetDataHandler(context).process();
            if (requestData.isEmpty()) {
                throw new ResourceNotFoundException();
            }
            IResponseSerializer jsonSerializer = AbstractFactory.getInstance().getSerializerFactory()
                    .createService("JSON", context);
            BaseResponsePayloadImpl baseResponsePayload = new BaseResponsePayloadImpl();
            baseResponsePayload.setPayload(requestData.get());
            baseResponsePayload.setPageCount(1);
            baseResponsePayload.setPageIndex(0);
            jsonSerializer.writeResponse(baseResponsePayload, response);
            response.setContentType(jsonSerializer.getContentType());
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            response.setStatus(ExceptionUtils.getStatusFromException(e));
            logger.error("Exception at get {} ", request.getRequestURI(), e);
        } finally {
            if (entityManager != null)
                entityManager.close();
            response.getWriter().flush();
        }
    }

    public void processPOSTRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManager entityManager = null;
        try {
            entityManager = speedyConfiguration.createEntityManager();
            PostRequestContext context = new PostRequestContext(request, metaModelProcessor, entityManager);
            new PostRequestParser(context).processBatch();
            new CreateDataHandler(context).processBatch();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            response.setStatus(ExceptionUtils.getStatusFromException(e));
            logger.error("Exception at get {} ", request.getRequestURI(), e);
        } finally {
            if (entityManager != null)
                entityManager.close();
            response.getWriter().flush();
        }
    }

    public void processPUTRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManager entityManager = null;
        try {
            entityManager = speedyConfiguration.createEntityManager();
            PutRequestContext context = new PutRequestContext(request, metaModelProcessor, entityManager);
            new PutRequestParser(context).process();
            new UpdateDataHandler(context).process();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            response.setStatus(ExceptionUtils.getStatusFromException(e));
            logger.error("Exception at get {} ", request.getRequestURI(), e);
        } finally {
            if (entityManager != null)
                entityManager.close();
            response.getWriter().flush();
        }
    }

    public void processDELETERequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManager entityManager = null;
        try {
            entityManager = speedyConfiguration.createEntityManager();
            DeleteRequestContext context = new DeleteRequestContext(request, metaModelProcessor, entityManager);
            new DeleteRequestParser(context).process();
            new DeleteDataHandler(context).process();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            response.setStatus(ExceptionUtils.getStatusFromException(e));
            logger.error("Exception at get {} ", request.getRequestURI(), e);
        } finally {
            if (entityManager != null)
                entityManager.close();
            response.getWriter().flush();
        }
    }


    public void requestResource(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (request.getMethod().equals(HttpMethod.GET.name())) {
                processGETRequests(request, response);
            } else if (request.getMethod().equals(HttpMethod.POST.name())) {
                processPOSTRequests(request, response);
            } else if (request.getMethod().equals(HttpMethod.PUT.name())) {
                processPUTRequests(request, response);
            } else if (request.getMethod().equals(HttpMethod.DELETE.name())) {
                processDELETERequests(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (Throwable e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(e.getLocalizedMessage());
        }


    }


}
