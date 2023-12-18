package com.github.silent.samurai.speedy.interfaces;

public interface SinglePayload {

    SpeedyValue getPayload();
    long getPageSize();
    long getPageIndex();

    long getTotalPageCount();

}
