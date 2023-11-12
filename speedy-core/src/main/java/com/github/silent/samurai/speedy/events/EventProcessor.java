package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.annotations.SpeedyEvent;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    private final MetaModelProcessor metaModelProcessor;
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
    private final Map<SpeedyEventType, Map<String, EventHandlerMetadata>> eventMap = new HashMap<>();

    public EventProcessor(MetaModelProcessor metaModelProcessor, RegistryImpl eventRegistry) {
        this.metaModelProcessor = metaModelProcessor;
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
                    Class<?> entityClass = annotation.value();
                    EntityMetadata entityMetadata = this.metaModelProcessor.findEntityMetadata(entityClass.getSimpleName());
                    for (SpeedyEventType event : SpeedyEventType.values()) {
                        eventMap.putIfAbsent(event, new HashMap<>());
                        Map<String, EventHandlerMetadata> eventEntityMap = eventMap.get(event);
                        if (Arrays.stream(annotation.eventType()).anyMatch(rt -> rt == event)) {
                            EventHandlerMetadata metadata = new EventHandlerMetadata(eventHandler, declaredMethod);
                            eventEntityMap.put(entityMetadata.getName(), metadata);
                        }
                    }
                }
            } catch (NotFoundException e) {
                LOGGER.warn("Exception during validation capture ", e);
            }
        }
    }

    public Object triggerEvent(SpeedyEventType eventType, EntityMetadata entityMetadata, Object entity) throws Exception {
        Map<String, EventHandlerMetadata> eventEntityMap = eventMap.get(eventType);
        if (eventEntityMap.containsKey(entityMetadata.getName())) {
            EventHandlerMetadata metadata = eventEntityMap.get(entityMetadata.getName());
            return metadata.invokeEventHandler(entity);
        }
        return null;
    }

    public boolean isEventPresent(SpeedyEventType eventType, EntityMetadata entityMetadata) {
        Map<String, EventHandlerMetadata> eventEntityMap = eventMap.get(eventType);
        return eventEntityMap.containsKey(entityMetadata.getName());
    }

    static class EventHandlerMetadata {
        Object instance;
        Method method;

        public EventHandlerMetadata(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        private Object invokeEventHandler(Object entity) throws Exception {
            return method.invoke(instance, entity);
        }
    }


}
