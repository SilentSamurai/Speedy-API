package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface SpeedyEngine {

    QueryProcessor prepare() throws SpeedyHttpException;

    SpeedyContext newContext(HttpServletRequest request, HttpServletResponse response) throws SpeedyHttpException;

    void parseRequest(SpeedyContext ctx) throws SpeedyHttpException;

    void selectBodyParser(SpeedyContext ctx) throws SpeedyHttpException;

    void parseBody(SpeedyContext ctx) throws SpeedyHttpException;

    void execute(SpeedyContext ctx) throws SpeedyHttpException;

    void selectSerializer(SpeedyContext ctx) throws SpeedyHttpException;

    void writeResponse(SpeedyContext ctx) throws SpeedyHttpException;
}
