package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.math.BigInteger;
import java.util.List;

public record QueryResult(List<SpeedyEntity> entities, BigInteger totalCount) {

}
