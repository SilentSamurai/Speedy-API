package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.interfaces.ISpeedyRegistry;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

@Getter
public class RegistryImpl implements ISpeedyRegistry {

    private final List<ISpeedyEventHandler> eventHandlers = new LinkedList<>();

    private final List<SpeedyVirtualEntityHandler> virtualEntityHandlers = new LinkedList<>();

    @Override
    public ISpeedyRegistry registerEventHandler(ISpeedyEventHandler eventHandler) {
        eventHandlers.add(eventHandler);
        return this;
    }

    @Override
    public ISpeedyRegistry registerVirtualEntityHandler(SpeedyVirtualEntityHandler virtualEntityHandler) {
        virtualEntityHandlers.add(virtualEntityHandler);
        return this;
    }

}
