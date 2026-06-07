package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.request.SpeedyRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface SpeedyEngine {

    QueryProcessor prepare() throws SpeedyHttpException;

    SpeedyRequest parseRequest(HttpServletRequest request) throws SpeedyHttpException;

    IRequestBodyParser selectBodyParser(SpeedyRequest request) throws SpeedyHttpException;

    SpeedyBody parseBody(IRequestBodyParser parser, SpeedyRequest request, QueryProcessor qp) throws SpeedyHttpException;

    SpeedyResponse get(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException;

    SpeedyResponse query(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException;

    SpeedyResponse create(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException;

    SpeedyResponse update(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException;

    SpeedyResponse delete(SpeedyRequest request, SpeedyBody body, QueryProcessor qp) throws SpeedyHttpException;

    IResponseSerializerV2 selectSerializer(HttpServletRequest servletRequest, SpeedyRequest speedyRequest) throws SpeedyHttpException;

    void writeResponse(IResponseSerializerV2 serializer, SpeedyResponse response, HttpServletResponse servletResponse) throws SpeedyHttpException;
}
