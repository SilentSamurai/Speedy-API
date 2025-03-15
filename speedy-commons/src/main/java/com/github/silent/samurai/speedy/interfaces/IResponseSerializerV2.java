package com.github.silent.samurai.speedy.interfaces;


import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.math.BigInteger;
import java.util.List;

public interface IResponseSerializerV2 {
    int SINGLE_ENTITY = 0;
    int MULTIPLE_ENTITY = 1;

    String getContentType();

    void write(IResponseContext context) throws SpeedyHttpException;

}
