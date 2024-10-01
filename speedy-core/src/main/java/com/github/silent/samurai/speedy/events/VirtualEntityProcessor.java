package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class VirtualEntityProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualEntityProcessor.class);

    private final MetaModelProcessor metaModelProcessor;
    private final RegistryImpl eventRegistry;


    private final Map<String, SpeedyVirtualEntityHandler> virtualEntityHandlerMap = new HashMap<>();

    public VirtualEntityProcessor(MetaModelProcessor metaModelProcessor, RegistryImpl eventRegistry) {
        this.metaModelProcessor = metaModelProcessor;
        this.eventRegistry = eventRegistry;
    }

    public void processRegistry() {
        for (RegistryImpl.VEHHoldr holdr : eventRegistry.getVirtualEntityHandlers()) {
            try {
                EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(holdr.getEntityClass().getSimpleName());
                virtualEntityHandlerMap.put(entityMetadata.getName(), holdr.getHandler());
            } catch (NotFoundException e) {
                LOGGER.error("entityMetadata not found ", e);
            }
        }
    }

    public boolean isVirtualEntity(EntityMetadata entityMetadata) {
        return virtualEntityHandlerMap.containsKey(entityMetadata.getName());
    }

    public SpeedyVirtualEntityHandler getHandler(EntityMetadata entityMetadata) {
        return virtualEntityHandlerMap.get(entityMetadata.getName());
    }

}
