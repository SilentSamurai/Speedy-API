package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;

import javax.servlet.http.HttpServletResponse;

public interface ResponseReturningRequestContext extends RequestContext {
    int getSerializationType();

    HttpServletResponse getResponse();

    SpeedyQuery getQuery();


}
