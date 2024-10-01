package com.github.silent.samurai.speedy.interfaces;

import java.util.List;

public interface MultiPayload {

    List<? extends SpeedyValue> getPayload();

    long getPageSize();

    long getPageIndex();

    long getTotalPageCount();

}
