package com.github.silent.samurai.speedy.interfaces;

public interface ISpeedyRegistry {

    ISpeedyRegistry registerEventHandler(ISpeedyEventHandler eventHandler);

    ISpeedyRegistry registerVirtualEntity(String entityName);

    ISpeedyRegistry registerValidator(ISpeedyCustomValidation validator);
}
