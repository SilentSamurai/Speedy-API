package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.annotations.SpeedyEvent;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.mappings.SpeedyDeserializer;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.mappings.SpeedySerializer;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    private final MetaModel metaModel;
    private final RegistryImpl eventRegistry;

    /*
    {
        "POST_INSERT": {
            Category: handler,
            Customer: handler
        },
        "POST_UPDATE": {
            Category: handler,
            Customer: handler
        }
    }
     */
    private final Map<SpeedyEventType, MultiValueMap<String, EventHandlerMetadata>> eventMap = new HashMap<>();

    public EventProcessor(MetaModel metaModel, RegistryImpl eventRegistry) {
        this.metaModel = metaModel;
        this.eventRegistry = eventRegistry;
    }

    public void processRegistry() {
        for (ISpeedyEventHandler eventHandler : eventRegistry.getEventHandlers()) {
            captureEvents(eventHandler);
        }
    }

    private void captureEvents(ISpeedyEventHandler eventHandler) {
        Class<? extends ISpeedyEventHandler> instance = eventHandler.getClass();
        for (Method declaredMethod : instance.getDeclaredMethods()) {
            try {
                if (declaredMethod.isAnnotationPresent(SpeedyEvent.class)) {
                    SpeedyEvent annotation = declaredMethod.getAnnotation(SpeedyEvent.class);
                    String entity = annotation.value();
                    Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
                    if (parameterTypes.length != 1) {
                        throw new IllegalArgumentException("Method " + declaredMethod.getName() + " must have a single parameter");
                    }
                    Class<?> ioClass = parameterTypes[0];
                    EntityMetadata entityMetadata = this.metaModel.findEntityMetadata(entity);
                    for (SpeedyEventType event : SpeedyEventType.values()) {
                        eventMap.putIfAbsent(event, new LinkedMultiValueMap<>());
                        MultiValueMap<String, EventHandlerMetadata> eventEntityMap = eventMap.get(event);
                        if (Arrays.stream(annotation.eventType()).anyMatch(rt -> rt == event)) {
                            EventHandlerMetadata metadata = new EventHandlerMetadata(eventHandler, declaredMethod, ioClass);
                            eventEntityMap.add(entityMetadata.getName(), metadata);
                        }
                    }
                }
            } catch (NotFoundException e) {
                LOGGER.warn("Exception during validation capture ", e);
            }
        }
    }

    public void triggerEvent(SpeedyEventType eventType, EntityMetadata entityMetadata, SpeedyEntity entity) throws Exception {
        if (isEventPresent(eventType, entityMetadata)) {
            MultiValueMap<String, EventHandlerMetadata> eventEntityMap = eventMap.get(eventType);
            for (EventHandlerMetadata metadata : eventEntityMap.get(entityMetadata.getName())) {
                metadata.invokeEventHandler(entity);
            }
        }
    }

    public boolean isEventPresent(SpeedyEventType eventType, EntityMetadata entityMetadata) {
        MultiValueMap<String, EventHandlerMetadata> eventEntityMap = eventMap.get(eventType);
        return eventEntityMap != null && eventEntityMap.containsKey(entityMetadata.getName()) &&
                !eventEntityMap.get(entityMetadata.getName()).isEmpty();
    }

    static class EventHandlerMetadata {
        final Object instance;
        final Method method;
        final Class<?> ioClass;

        public EventHandlerMetadata(Object instance, Method method, Class<?> ioClass) {
            this.instance = instance;
            this.method = method;
            this.ioClass = ioClass;
        }

        private Object invokeEventHandler(SpeedyEntity entity) throws Exception {
            try {
                if (ioClass.isAssignableFrom(SpeedyEntity.class)) {
                    method.invoke(instance, entity);
                } else {
                    Object value = SpeedySerializer.toJavaEntity(entity, ioClass);
                    method.invoke(instance, value);
                    SpeedyDeserializer.updateEntity(value, entity);
                }
            } catch (InvocationTargetException ite) {
                // Surface the underlying exception if it's a SpeedyHttpException so HTTP status can propagate
                if (ite.getCause() instanceof SpeedyHttpException she) {
                    throw she;
                }
                throw ite;
            }
            return entity;
        }
    }


}
