package com.github.silent.samurai.speedy.interfaces;

public interface ISpeedyExceptionMapper {

    int getStatus(Throwable throwable);

    String getMessage(Throwable throwable);
}
