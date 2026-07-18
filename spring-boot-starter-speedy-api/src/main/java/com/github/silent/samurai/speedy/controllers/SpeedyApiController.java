package com.github.silent.samurai.speedy.controllers;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Hidden
@RestController
@RequestMapping(SpeedyConstants.URI)
public class SpeedyApiController {

    private final SpeedyFactory speedyFactory;

    public SpeedyApiController(SpeedyFactory speedyFactory) {
        this.speedyFactory = speedyFactory;
    }

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
