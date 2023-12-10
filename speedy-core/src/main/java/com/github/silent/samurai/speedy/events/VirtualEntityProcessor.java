package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class VirtualEntityProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualEntityProcessor.class);

    private final MetaModelProcessor metaModelProcessor;
    private final RegistryImpl eventRegistry;


    private final Map<String, SpeedyVirtualEntityHandler> virtualEntityHandlerMap = new HashMap<>();

    public VirtualEntityProcessor(MetaModelProcessor metaModelProcessor, RegistryImpl eventRegistry) {
        this.metaModelProcessor = metaModelProcessor;
        this.eventRegistry = eventRegistry;
    }

    public static Optional<Type> getTypeParameters(Class<?> clazz) {
        Type[] genericSuperclass = clazz.getGenericInterfaces();

        Optional<Type> typeOptional = Optional.empty();
        for (Type inf : genericSuperclass) {
            if (inf instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) inf;
                if (SpeedyVirtualEntityHandler.class.isAssignableFrom((Class<?>) type.getRawType())) {
                    Type[] actualTypeArguments = type.getActualTypeArguments();
                    typeOptional = Optional.of(actualTypeArguments[0]);
                    break;
                }
            }
        }
        // If the class is not a parameterized type, return an empty array
        return typeOptional;
    }

    public void processRegistry() {
        for (SpeedyVirtualEntityHandler virtualEntityHandler : eventRegistry.getVirtualEntityHandlers()) {
            Optional<Type> typeParameters = getTypeParameters(virtualEntityHandler.getClass());


            if (typeParameters.isPresent() && metaModelProcessor.hasEntityMetadata((Class<?>) typeParameters.get())) {
                Class<?> aClass = (Class<?>) typeParameters.get();
                try {
                    EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(aClass);
                    virtualEntityHandlerMap.put(entityMetadata.getName(), virtualEntityHandler);
                } catch (NotFoundException e) {
                    LOGGER.error("typeName not found ", e);
                }
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
