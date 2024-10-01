package com.github.silent.samurai.speedy.interfaces;

public interface ISpeedyRegistry {

    ISpeedyRegistry registerEventHandler(ISpeedyEventHandler eventHandler);

    ISpeedyRegistry registerVirtualEntityHandler(SpeedyVirtualEntityHandler virtualEntityHandler, Class<?> entityClass);

    ISpeedyRegistry registerValidator(ISpeedyCustomValidation validator);
}
