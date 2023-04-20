package com.github.silent.samurai.controllers;


import com.github.silent.samurai.SpeedyFactory;
import com.github.silent.samurai.interfaces.Constants;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.serializers.MetaModelSerializer;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@RestController
@RequestMapping(Constants.URI)
public class SpeedyApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyApiController.class);

    @Autowired
    SpeedyFactory speedyFactory;

    @RequestMapping(value = "/$metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    public String metadata() {
        MetaModelProcessor metaModelProcessor = speedyFactory.getMetaModelProcessor();
        JsonElement jsonElement = MetaModelSerializer.serializeMetaModel(metaModelProcessor);
        return CommonUtil.getGson().toJson(jsonElement);
    }

    @RequestMapping(value = "*")
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, InvocationTargetException, IllegalAccessException {
        speedyFactory.requestResource(request, response);
    }
}
