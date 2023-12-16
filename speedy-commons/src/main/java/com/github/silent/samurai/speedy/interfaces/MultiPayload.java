package com.github.silent.samurai.speedy.interfaces;

import java.util.List;

public interface MultiPayload {

    List<? extends SpeedyValue> getPayload();

    int getPageCount();

    int getPageIndex();

}
