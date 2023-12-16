package com.github.silent.samurai.speedy.interfaces;


public interface IResponseSerializer {
    int SINGLE_ENTITY = 0;
    int MULTIPLE_ENTITY = 1;

    String getContentType();

    ResponseReturningRequestContext getContext();

    void writeResponse(SinglePayload singlePayload) throws Exception;

    void writeResponse(MultiPayload multiPayload) throws Exception;

}
