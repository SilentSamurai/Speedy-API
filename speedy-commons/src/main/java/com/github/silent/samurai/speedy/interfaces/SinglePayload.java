package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.models.SpeedyEntity;

public interface SinglePayload {

    SpeedyValue getPayload();
    int getPageCount();

    int getPageIndex();

}
