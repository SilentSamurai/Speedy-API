package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.annotations.SpeedyEvent;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.conversion.walker.java.JavaToSpeedy;
import com.github.silent.samurai.speedy.conversion.walker.java.SpeedyToJava;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    private final MetaModel metaModel;
    private final RegistryImpl eventRegistry;
    /// Shared serializer for converting SpeedyEntity to user POJOs before
    /// invoking event handler methods.
    ///
    /// @see SpeedyToJava#toJavaEntity
    private final SpeedyToJava serializer;
    /// Shared deserializer for synchronizing changes from user POJOs back to
    /// SpeedyEntity after event handler methods return.
    ///
    /// @see JavaToSpeedy#updateEntity
    private final JavaToSpeedy deserializer;

    private final Map<SpeedyEventType, MultiValueMap<String, EventHandlerMetadata>> eventMap = new HashMap<>();

    /// Creates the event processor with the necessary serialization infrastructure.
    ///
    /// @param metaModel     the global metamodel
    /// @param eventRegistry the registry holding user-registered event handlers
    /// @param serializer    serializer for {@code SpeedyEntity -> POJO}
    /// @param deserializer  deserializer for {@code POJO -> SpeedyEntity}
    public EventProcessor(MetaModel metaModel, RegistryImpl eventRegistry, SpeedyToJava serializer, JavaToSpeedy deserializer) {
        this.metaModel = metaModel;
        this.eventRegistry = eventRegistry;
        this.serializer = serializer;
        this.deserializer = deserializer;
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
                            try {
                                MethodHandle mh = MethodHandles.lookup().unreflect(declaredMethod);
                                EventHandlerMetadata metadata = new EventHandlerMetadata(eventHandler, mh, ioClass);
                                eventEntityMap.add(entityMetadata.getName(), metadata);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Cannot access event handler method " + declaredMethod.getName(), e);
                            }
                        }
                    }
                }
            } catch (NotFoundException e) {
                throw new RuntimeException("Misconfigured @SpeedyEvent: entity '" + declaredMethod.getAnnotation(SpeedyEvent.class).value() + "' not found in metamodel", e);
            }
        }
    }

    public void triggerEvent(SpeedyEventType eventType, EntityMetadata entityMetadata, SpeedyEntity entity) throws Exception {
        if (isEventPresent(eventType, entityMetadata)) {
            MultiValueMap<String, EventHandlerMetadata> eventEntityMap = eventMap.get(eventType);
            for (EventHandlerMetadata metadata : eventEntityMap.get(entityMetadata.getName())) {
                metadata.invokeEventHandler(entity, serializer, deserializer);
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
        final MethodHandle methodHandle;
        final Class<?> ioClass;

        public EventHandlerMetadata(Object instance, MethodHandle methodHandle, Class<?> ioClass) {
            this.instance = instance;
            this.methodHandle = methodHandle;
            this.ioClass = ioClass;
        }

        private Object invokeEventHandler(SpeedyEntity entity, SpeedyToJava ser, JavaToSpeedy deser) throws Exception {
            try {
                if (ioClass.isAssignableFrom(SpeedyEntity.class)) {
                    methodHandle.invoke(instance, entity);
                } else {
                    Object value = ser.toJavaEntity(entity, ioClass);
                    methodHandle.invoke(instance, value);
                    deser.updateEntity(value, entity);
                }
            } catch (Throwable t) {
                if (t instanceof Exception e) {
                    throw e;
                }
                throw new RuntimeException(t);
            }
            return entity;
        }
    }


}
