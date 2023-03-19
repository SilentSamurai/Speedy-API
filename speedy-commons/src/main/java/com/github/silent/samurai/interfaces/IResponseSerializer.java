package com.github.silent.samurai.interfaces;

import javax.servlet.http.HttpServletResponse;

public interface IResponseSerializer {
    int SINGLE_ENTITY = 0;
    int MULTIPLE_ENTITY = 1;

    String getContentType();

    void writeResponse(Object requestedObject, HttpServletResponse response) throws Exception;
}
