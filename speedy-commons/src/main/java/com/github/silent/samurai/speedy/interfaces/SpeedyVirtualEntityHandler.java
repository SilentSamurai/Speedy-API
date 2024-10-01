package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

public interface SpeedyVirtualEntityHandler {

    SpeedyEntity create(SpeedyEntity entity) throws NotFoundException;

    SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws NotFoundException;

    SpeedyEntity delete(SpeedyEntityKey pk) throws NotFoundException;

}
