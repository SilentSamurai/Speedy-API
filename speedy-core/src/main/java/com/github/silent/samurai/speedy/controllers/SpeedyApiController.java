package com.github.silent.samurai.speedy.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.docs.MetaModelSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping(value = "/$metadata", produces = "application/json")
    public String metadata() throws JsonProcessingException {
        MetaModel metaModel = speedyFactory.getMetaModel();
        JsonNode jsonElement = MetaModelSerializer.serializeMetaModel(metaModel);
        return CommonUtil.json().writeValueAsString(jsonElement);
    }

    @Hidden
    @RequestMapping(value = {"*", "/**", "*/*"})
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, InvocationTargetException, IllegalAccessException {
//        speedyFactory.processRequest(request, response);
        speedyFactory.processReqV2(request, response);
    }
}
