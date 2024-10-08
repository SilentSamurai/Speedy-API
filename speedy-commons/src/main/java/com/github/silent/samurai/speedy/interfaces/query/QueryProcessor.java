package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.util.List;

public interface QueryProcessor {

    SpeedyEntity executeOne(SpeedyQuery query) throws SpeedyHttpException;

    List<SpeedyEntity> executeMany(SpeedyQuery query) throws SpeedyHttpException;

    boolean exists(SpeedyEntityKey entityKey) throws SpeedyHttpException;

    SpeedyEntity create(SpeedyEntity entity) throws SpeedyHttpException;

    SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException;

    SpeedyEntity delete(SpeedyEntityKey entityKey) throws SpeedyHttpException;


}
