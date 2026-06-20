package com.github.silent.samurai.speedy.controllers;


import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Hidden
@RestController
@RequestMapping(SpeedyConstant.URI)
public class SpeedyApiController {

    @Autowired
    SpeedyFactory speedyFactory;

    @Hidden
    @GetMapping(value = "/$metadata")
    public void metadata(HttpServletRequest request, HttpServletResponse response) throws IOException {
        speedyFactory.processReqV2(request, response);
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
