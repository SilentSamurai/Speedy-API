package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

public interface SpeedyVirtualEntityHandler {

    SpeedyEntity create(SpeedyEntity entity);

    SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity);

    SpeedyEntity delete(SpeedyEntityKey pk);

}
