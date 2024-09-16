package com.github.silent.samurai.speedy.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.docs.MetaModelSerializer;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.query.QueryProcessorImpl;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.query.JsonQueryBuilder;
import com.github.silent.samurai.speedy.request.get.GetRequestContext;
import com.github.silent.samurai.speedy.serializers.JSONSerializer;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import com.github.silent.samurai.speedy.utils.ExceptionUtils;
import io.swagger.v3.oas.annotations.Hidden;
import org.jooq.SQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

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


    private void queryRequest(HttpServletRequest request,
                              HttpServletResponse response,
                              Optional<String> from) throws Exception {
        MetaModelProcessor metaModelProcessor = speedyFactory.getMetaModelProcessor();

        DataSource dataSource = speedyFactory.getSpeedyConfiguration().getDataSource();
        String dialect = speedyFactory.getSpeedyConfiguration().getDialect();
        QueryProcessor queryProcessor = new QueryProcessorImpl(dataSource, SQLDialect.valueOf(dialect));
        try {

            JsonNode jsonQuery = CommonUtil.json().readTree(request.getReader());

            JsonQueryBuilder jsonQueryBuilder;

            if (from.isPresent()) {
                jsonQueryBuilder = new JsonQueryBuilder(metaModelProcessor, from.get(), jsonQuery);
            } else {
                jsonQueryBuilder = new JsonQueryBuilder(metaModelProcessor, jsonQuery);
            }

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

    @PostMapping(value = "/{from}/$query")
    public void query(HttpServletRequest request, HttpServletResponse response, @PathVariable("from") String from) throws Exception {
        queryRequest(request, response, Optional.of(from));
    }

    @PostMapping(value = "/$query")
    public void query(HttpServletRequest request, HttpServletResponse response) throws Exception {
        queryRequest(request, response, Optional.empty());
    }

    @Hidden
    @RequestMapping(value = {"*", "*/*"})
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, InvocationTargetException, IllegalAccessException {
        speedyFactory.requestResource(request, response);
    }
}
