package com.github.silent.samurai.interfaces;

import javax.servlet.http.HttpServletResponse;

public interface ResponseReturningRequestContext extends RequestContext {
    int getSerializationType();

    HttpServletResponse getResponse();
}
