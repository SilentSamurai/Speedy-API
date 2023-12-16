package com.github.silent.samurai.speedy.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.docs.MetaModelSerializer;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.query.JsonQueryBuilder;
import com.github.silent.samurai.speedy.request.get.GetRequestContext;
import com.github.silent.samurai.speedy.responses.MultiPayloadWrapper;
import com.github.silent.samurai.speedy.serializers.JSONSerializer;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import com.github.silent.samurai.speedy.utils.ExceptionUtils;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Hidden
@RestController
@RequestMapping(SpeedyConstant.URI)
public class SpeedyApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyApiController.class);

    @Autowired
    SpeedyFactory speedyFactory;

    @Hidden
    @GetMapping(value = "/$metadata", produces = "application/json")
    public String metadata() throws JsonProcessingException {
        MetaModelProcessor metaModelProcessor = speedyFactory.getMetaModelProcessor();
        JsonNode jsonElement = MetaModelSerializer.serializeMetaModel(metaModelProcessor);
        return CommonUtil.json().writeValueAsString(jsonElement);
    }

    @PostMapping(value = "/$query")
    public void query(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MetaModelProcessor metaModelProcessor = speedyFactory.getMetaModelProcessor();
        QueryProcessor queryProcessor = metaModelProcessor.getQueryProcessor();
        try {
            JsonNode jsonQuery = CommonUtil.json().readTree(request.getReader());

            JsonQueryBuilder jsonQueryBuilder = new JsonQueryBuilder(metaModelProcessor, jsonQuery);
            SpeedyQuery speedyQuery = jsonQueryBuilder.build();

            GetRequestContext context = new GetRequestContext(request, response, metaModelProcessor);

            List<SpeedyEntity> speedyEntities = queryProcessor.executeMany(speedyQuery);
            context.setSpeedyQuery(speedyQuery);

            IResponseSerializer jsonSerializer = new JSONSerializer(context);
            jsonSerializer.write(speedyEntities);

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
            metaModelProcessor.closeQueryProcessor(queryProcessor);
        }
    }

    @Hidden
    @RequestMapping(value = {"*", "*/*"})
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, InvocationTargetException, IllegalAccessException {
        speedyFactory.requestResource(request, response);
    }
}
