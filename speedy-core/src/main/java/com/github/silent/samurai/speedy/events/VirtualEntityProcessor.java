package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VirtualEntityProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualEntityProcessor.class);

    private final MetaModelProcessor metaModelProcessor;
    private final RegistryImpl eventRegistry;


    private Set<String> virtualEntities = Set.of();

    public VirtualEntityProcessor(MetaModelProcessor metaModelProcessor, RegistryImpl eventRegistry) {
        this.metaModelProcessor = metaModelProcessor;
        this.eventRegistry = eventRegistry;
    }

    public void processRegistry() {
        virtualEntities = eventRegistry.getVirtualEntities();
    }

    public boolean isVirtualEntity(EntityMetadata entityMetadata) {
        return virtualEntities.contains(entityMetadata.getName());
    }

}
