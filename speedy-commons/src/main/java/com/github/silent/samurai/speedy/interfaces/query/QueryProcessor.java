package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.interfaces.SpeedyEntity;

import java.util.List;

public interface QueryProcessor {

    SpeedyEntity executeOne(SpeedyQuery query);

    List<SpeedyEntity> executeMany(SpeedyQuery query) throws Exception;
}
