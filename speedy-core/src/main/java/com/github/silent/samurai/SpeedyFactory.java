package com.github.silent.samurai;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.exceptions.ResourceNotFoundException;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.request.get.GETRequestContext;
import com.github.silent.samurai.request.post.POSTRequestProcessor;
import com.github.silent.samurai.request.get.GETRequestParser;
import com.github.silent.samurai.response.ResponseProcessor;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class SpeedyFactory {

    Logger logger = LogManager.getLogger(SpeedyFactory.class);

    private final EntityManagerFactory entityManagerFactory;
    private final MetaModelProcessor metaModelProcessor;


    public SpeedyFactory(EntityManagerFactory entityManagerFactory, MetaModelProcessor metaModelProcessor) {
        this.entityManagerFactory = entityManagerFactory;
        this.metaModelProcessor = metaModelProcessor;
    }

    public void processGETRequests(HttpServletRequest request, HttpServletResponse response)
            throws IOException, InvocationTargetException, IllegalAccessException {
        GETRequestContext GETRequestContext = new GETRequestParser(metaModelProcessor).process(request);
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        JsonElement jsonElement = new ResponseProcessor(metaModelProcessor).process(GETRequestContext, entityManager);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);
        Gson gson = CommonUtil.getGson();
        gson.toJson(jsonElement, response.getWriter());

        response.getWriter().flush();
        entityManager.close();
    }

    public void processPOSTRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        String body = CharStreams.toString(request.getReader());
        Gson gson = CommonUtil.getGson();
        JsonElement jsonElement = gson.fromJson(request.getReader(), JsonElement.class);
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        new POSTRequestProcessor(metaModelProcessor, entityManager).process(jsonElement);

//        logger.info("test {}", jsonElement);
        entityManager.close();
    }


    public void requestResource(HttpServletRequest request, HttpServletResponse response)
            throws IOException, InvocationTargetException, IllegalAccessException {
        try {
            if (request.getMethod().equals(HttpMethod.GET.name())) {
                processGETRequests(request, response);
            } else if (request.getMethod().equals(HttpMethod.POST.name())) {
                processPOSTRequests(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (BadRequestException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(e.getLocalizedMessage());
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(e.getLocalizedMessage());
        }


    }


}
