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
    private final EventRegistryImpl eventRegistry;
    private final Map<String, EventHandlerMetadata> preInsertEventHandler = new HashMap<>();
    private final Map<String, EventHandlerMetadata> preUpdateEventHandler = new HashMap<>();
    private final Map<String, EventHandlerMetadata> preDeleteEventHandler = new HashMap<>();
    private final Map<String, EventHandlerMetadata> postInsertEventHandler = new HashMap<>();
    private final Map<String, EventHandlerMetadata> postUpdateEventHandler = new HashMap<>();
    private final Map<String, EventHandlerMetadata> postDeleteEventHandler = new HashMap<>();

    public EventProcessor(MetaModelProcessor metaModelProcessor, EventRegistryImpl eventRegistry) {
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
                    if (Arrays.stream(annotation.eventType()).anyMatch(rt -> rt == SpeedyEventType.PRE_INSERT)) {
                        EventHandlerMetadata metadata = new EventHandlerMetadata(eventHandler, declaredMethod);
                        preInsertEventHandler.put(entityMetadata.getName(), metadata);
                    }
                    if (Arrays.stream(annotation.eventType()).anyMatch(rt -> rt == SpeedyEventType.PRE_UPDATE)) {
                        EventHandlerMetadata metadata = new EventHandlerMetadata(eventHandler, declaredMethod);
                        preUpdateEventHandler.put(entityMetadata.getName(), metadata);
                    }
                    if (Arrays.stream(annotation.eventType()).anyMatch(rt -> rt == SpeedyEventType.PRE_DELETE)) {
                        EventHandlerMetadata metadata = new EventHandlerMetadata(eventHandler, declaredMethod);
                        preDeleteEventHandler.put(entityMetadata.getName(), metadata);
                    }
                    if (Arrays.stream(annotation.eventType()).anyMatch(rt -> rt == SpeedyEventType.POST_INSERT)) {
                        EventHandlerMetadata metadata = new EventHandlerMetadata(eventHandler, declaredMethod);
                        postInsertEventHandler.put(entityMetadata.getName(), metadata);
                    }
                    if (Arrays.stream(annotation.eventType()).anyMatch(rt -> rt == SpeedyEventType.POST_UPDATE)) {
                        EventHandlerMetadata metadata = new EventHandlerMetadata(eventHandler, declaredMethod);
                        postUpdateEventHandler.put(entityMetadata.getName(), metadata);
                    }
                    if (Arrays.stream(annotation.eventType()).anyMatch(rt -> rt == SpeedyEventType.POST_DELETE)) {
                        EventHandlerMetadata metadata = new EventHandlerMetadata(eventHandler, declaredMethod);
                        postDeleteEventHandler.put(entityMetadata.getName(), metadata);
                    }
                }
            } catch (NotFoundException e) {
                LOGGER.warn("Exception during validation capture ", e);
            }
        }
    }

    public void triggerPreInsertEvent(EntityMetadata entityMetadata, Object entity) throws Exception {
        if (preInsertEventHandler.containsKey(entityMetadata.getName())) {
            EventHandlerMetadata metadata = preInsertEventHandler.get(entityMetadata.getName());
            metadata.invokeEventHandler(entity);
        }
    }

    public void triggerPreUpdateEvent(EntityMetadata entityMetadata, Object entity) throws Exception {
        if (preUpdateEventHandler.containsKey(entityMetadata.getName())) {
            EventHandlerMetadata metadata = preUpdateEventHandler.get(entityMetadata.getName());
            metadata.invokeEventHandler(entity);
        }
    }

    public void triggerPreDeleteEvent(EntityMetadata entityMetadata, Object entity) throws Exception {
        if (preDeleteEventHandler.containsKey(entityMetadata.getName())) {
            EventHandlerMetadata metadata = preDeleteEventHandler.get(entityMetadata.getName());
            metadata.invokeEventHandler(entity);
        }
    }

    public void triggerPostInsertEvent(EntityMetadata entityMetadata, Object entity) throws Exception {
        if (postInsertEventHandler.containsKey(entityMetadata.getName())) {
            EventHandlerMetadata metadata = postInsertEventHandler.get(entityMetadata.getName());
            metadata.invokeEventHandler(entity);
        }
    }

    public void triggerPostUpdateEvent(EntityMetadata entityMetadata, Object entity) throws Exception {
        if (postUpdateEventHandler.containsKey(entityMetadata.getName())) {
            EventHandlerMetadata metadata = postUpdateEventHandler.get(entityMetadata.getName());
            metadata.invokeEventHandler(entity);
        }
    }

    public void triggerPostDeleteEvent(EntityMetadata entityMetadata, Object entity) throws Exception {
        if (postDeleteEventHandler.containsKey(entityMetadata.getName())) {
            EventHandlerMetadata metadata = postDeleteEventHandler.get(entityMetadata.getName());
            metadata.invokeEventHandler(entity);
        }
    }

    static class EventHandlerMetadata {
        Object instance;
        Method method;

        public EventHandlerMetadata(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        private void invokeEventHandler(Object entity) throws Exception {
            Object valid = method.invoke(instance, entity);
        }
    }


}
