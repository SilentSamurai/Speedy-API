package com.github.silent.samurai.controllers;


import com.github.silent.samurai.interfaces.Constants;
import com.github.silent.samurai.SpeedyFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@RestController
@RequestMapping(Constants.URI)
public class SpeedyApiController {

    Logger logger = LogManager.getLogger(SpeedyApiController.class);

    @Autowired
    SpeedyFactory speedyFactory;

    @RequestMapping(value = "*")
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, InvocationTargetException, IllegalAccessException {
        speedyFactory.requestResource(request, response);
    }
}
