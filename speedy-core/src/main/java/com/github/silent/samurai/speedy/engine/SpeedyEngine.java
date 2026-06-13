package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.request.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface SpeedyEngine {

    QueryProcessor prepare() throws SpeedyHttpException;

    RequestContext newContext(HttpServletRequest request, HttpServletResponse response) throws SpeedyHttpException;

    void parseRequest(RequestContext ctx) throws SpeedyHttpException;

    void selectBodyParser(RequestContext ctx) throws SpeedyHttpException;

    void parseBody(RequestContext ctx) throws SpeedyHttpException;

    void execute(RequestContext ctx) throws SpeedyHttpException;

    void selectSerializer(RequestContext ctx) throws SpeedyHttpException;

    void writeResponse(RequestContext ctx) throws SpeedyHttpException;
}
