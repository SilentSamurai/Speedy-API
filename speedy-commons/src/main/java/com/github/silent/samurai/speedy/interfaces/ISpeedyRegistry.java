package com.github.silent.samurai.speedy.interfaces;

public interface ISpeedyRegistry {

    ISpeedyRegistry registerEventHandler(ISpeedyEventHandler eventHandler);

    ISpeedyRegistry registerValidator(ISpeedyCustomValidation validator);
}
