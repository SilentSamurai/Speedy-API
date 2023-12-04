package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.util.List;

public interface QueryProcessor {

    SpeedyEntity executeOne(SpeedyQuery query) throws Exception;

    List<SpeedyEntity> executeMany(SpeedyQuery query) throws Exception;

    boolean exists(SpeedyEntityKey entityKey);

    boolean create(SpeedyEntity entity);

    boolean update(SpeedyEntity entity);

    boolean delete(SpeedyEntityKey entityKey);


}
