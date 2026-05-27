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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Hidden
@RestController
@RequestMapping(SpeedyConstant.URI)
public class SpeedyApiController {

    @Autowired
    SpeedyFactory speedyFactory;

    @Hidden
    @GetMapping(value = "/$metadata", produces = "application/json")
    public String metadata() throws JsonProcessingException {
        if (!speedyFactory.getConfiguration().isMetadataEndpointEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        MetaModel metaModel = speedyFactory.getMetaModel();
        JsonNode jsonElement = MetaModelSerializer.serializeMetaModel(metaModel);
        return CommonUtil.json().writeValueAsString(jsonElement);
    }

    @Hidden
    @GetMapping("{entity}/**")
    public void processGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        speedyFactory.processReqV2(request, response);
    }

    @Hidden
    @PostMapping("{entity}/**")
    public void processPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        speedyFactory.processReqV2(request, response);
    }

    @Hidden
    @PutMapping("{entity}/**")
    public void processPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        speedyFactory.processReqV2(request, response);
    }

    @Hidden
    @PatchMapping("{entity}/**")
    public void processPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        speedyFactory.processReqV2(request, response);
    }

    @Hidden
    @DeleteMapping("{entity}/**")
    public void processDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        speedyFactory.processReqV2(request, response);
    }
}
