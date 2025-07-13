package com.github.silent.samurai.speedy.interfaces;


import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;

public interface IResponseSerializerV2 {
    int SINGLE_ENTITY = 0;
    int MULTIPLE_ENTITY = 1;

    String getContentType();

    void write(IResponseContext context) throws SpeedyHttpException;

}
