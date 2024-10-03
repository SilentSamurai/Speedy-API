package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.interfaces.ISpeedyRegistry;
import lombok.Getter;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter
public class RegistryImpl implements ISpeedyRegistry {

    private final List<ISpeedyEventHandler> eventHandlers = new LinkedList<>();

    private final Set<String> virtualEntities = new HashSet<>();

    private final List<ISpeedyCustomValidation> validators = new LinkedList<>();

    @Override
    public ISpeedyRegistry registerEventHandler(ISpeedyEventHandler eventHandler) {
        eventHandlers.add(eventHandler);
        return this;
    }

    @Override
    public ISpeedyRegistry registerVirtualEntity(String entity) {
        virtualEntities.add(entity);
        return this;
    }

    @Override
    public ISpeedyRegistry registerValidator(ISpeedyCustomValidation validator) {
        validators.add(validator);
        return this;
    }
}
