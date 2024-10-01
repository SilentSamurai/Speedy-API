package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.interfaces.ISpeedyRegistry;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

@Getter
public class RegistryImpl implements ISpeedyRegistry {

    private final List<ISpeedyEventHandler> eventHandlers = new LinkedList<>();

    private final List<VEHHoldr> virtualEntityHandlers = new LinkedList<>();

    private final List<ISpeedyCustomValidation> validators = new LinkedList<>();

    @Override
    public ISpeedyRegistry registerEventHandler(ISpeedyEventHandler eventHandler) {
        eventHandlers.add(eventHandler);
        return this;
    }

    @Override
    public ISpeedyRegistry registerVirtualEntityHandler(SpeedyVirtualEntityHandler virtualEntityHandler, Class<?> entityClass) {
        virtualEntityHandlers.add(new VEHHoldr(virtualEntityHandler, entityClass));
        return this;
    }

    @Override
    public ISpeedyRegistry registerValidator(ISpeedyCustomValidation validator) {
        validators.add(validator);
        return this;
    }

    @Getter
    static class VEHHoldr {
        SpeedyVirtualEntityHandler handler;
        Class<?> entityClass;

        public VEHHoldr(SpeedyVirtualEntityHandler handler, Class<?> entityClass) {
            this.handler = handler;
            this.entityClass = entityClass;
        }
    }

}
