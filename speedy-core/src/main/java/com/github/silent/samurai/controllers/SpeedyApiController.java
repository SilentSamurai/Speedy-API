package com.github.silent.samurai.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.SpeedyFactory;
import com.github.silent.samurai.docs.MetaModelSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Hidden
@RestController
@RequestMapping(SpeedyConstant.URI)
public class SpeedyApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyApiController.class);

    @Autowired
    SpeedyFactory speedyFactory;


    @Hidden
    @GetMapping(value = "/$metadata")
    public String metadata() throws JsonProcessingException {
        MetaModelProcessor metaModelProcessor = speedyFactory.getMetaModelProcessor();
        JsonNode jsonElement = MetaModelSerializer.serializeMetaModel(metaModelProcessor);
        return CommonUtil.json().writeValueAsString(jsonElement);
    }

    @Hidden
    @RequestMapping(value = {"*", "*/*"})
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, InvocationTargetException, IllegalAccessException {
        speedyFactory.requestResource(request, response);
    }
}
