package com.github.silent.samurai.interfaces;


public interface IResponseSerializer {
    int SINGLE_ENTITY = 0;
    int MULTIPLE_ENTITY = 1;

    String getContentType();

    ResponseReturningRequestContext getContext();

    void writeResponse(IBaseResponsePayload requestedPayload) throws Exception;
}
