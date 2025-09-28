package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.mappings.ConversionFactory;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.math.BigInteger;
import java.util.List;

public interface QueryProcessor {

    BigInteger executeCount(SpeedyQuery query) throws SpeedyHttpException;

    List<SpeedyEntity> executeMany(SpeedyQuery query) throws SpeedyHttpException;

    boolean exists(SpeedyEntityKey entityKey) throws SpeedyHttpException;

    List<SpeedyEntity> create(List<SpeedyEntity> entities) throws SpeedyHttpException;

    SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException;

    List<SpeedyEntity> delete(List<SpeedyEntityKey> entityKeys) throws SpeedyHttpException;

    ConversionFactory getConversionProcessor();

}
