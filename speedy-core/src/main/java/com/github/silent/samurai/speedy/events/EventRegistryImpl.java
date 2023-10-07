package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventRegistry;

import java.util.LinkedList;
import java.util.List;

public class EventRegistryImpl implements ISpeedyEventRegistry {

    private final List<ISpeedyEventHandler> eventHandlers = new LinkedList<>();

    @Override
    public ISpeedyEventRegistry registerHandler(ISpeedyEventHandler eventHandler) {
        eventHandlers.add(eventHandler);
        return this;
    }

    public List<ISpeedyEventHandler> getEventHandlers() {
        return eventHandlers;
    }
}
