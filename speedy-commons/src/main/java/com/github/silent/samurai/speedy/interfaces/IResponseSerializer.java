package com.github.silent.samurai.speedy.interfaces;


import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.util.List;

public interface IResponseSerializer {
    int SINGLE_ENTITY = 0;
    int MULTIPLE_ENTITY = 1;

    String getContentType();

    IResponseContext getContext();

    void write(List<SpeedyEntity> speedyEntities) throws Exception;

    void write(SpeedyEntity speedyEntity) throws Exception;
}
