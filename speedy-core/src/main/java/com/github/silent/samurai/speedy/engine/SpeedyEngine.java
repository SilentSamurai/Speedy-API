package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.request.SpeedyRequest;

public interface SpeedyEngine {

    QueryProcessor prepare(RequestContext ctx) throws SpeedyHttpException;

    SpeedyRequest parseRequest(RequestContext ctx) throws SpeedyHttpException;

    SpeedyBody parseBody(RequestContext ctx) throws SpeedyHttpException;

    void selectBodyParser(RequestContext ctx) throws SpeedyHttpException;

    void selectSerializer(RequestContext ctx) throws SpeedyHttpException;

    SpeedyResponse get(RequestContext ctx) throws SpeedyHttpException;

    SpeedyResponse query(RequestContext ctx) throws SpeedyHttpException;

    SpeedyResponse create(RequestContext ctx) throws SpeedyHttpException;

    SpeedyResponse update(RequestContext ctx) throws SpeedyHttpException;

    SpeedyResponse delete(RequestContext ctx) throws SpeedyHttpException;

    void writeResponse(RequestContext ctx) throws SpeedyHttpException;
}
