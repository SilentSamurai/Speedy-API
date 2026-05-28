package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.annotations.SpeedyControllerAdvice;
import com.github.silent.samurai.speedy.annotations.SpeedyExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdviceExceptionMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdviceExceptionMapper.class);

    private final List<Object> controllerAdvices;
    private volatile Map<Class<? extends Throwable>, HandlerMethod> handlerCache;

    public AdviceExceptionMapper(List<Object> controllerAdvices) {
        this.controllerAdvices = controllerAdvices;
    }

    public int getStatus(Throwable throwable) {
        ResolutionResult result = resolveHandler(throwable);
        if (result != null) {
            return result.handler.httpStatus;
        }
        return -1;
    }

    public String getMessage(Throwable throwable) {
        ResolutionResult result = resolveHandler(throwable);
        if (result != null) {
            return invokeHandler(result.handler, result.matchedThrowable);
        }
        return null;
    }

    private ResolutionResult resolveHandler(Throwable throwable) {
        Map<Class<? extends Throwable>, HandlerMethod> cache = getCache();

        // Collect the cause chain
        List<Throwable> chain = new ArrayList<>();
        Throwable current = throwable;
        while (current != null) {
            chain.add(current);
            current = current.getCause();
        }

        // Walk from innermost to outermost (root cause first)
        Collections.reverse(chain);

        for (Throwable t : chain) {
            HandlerMethod handler = findHandlerForType(t.getClass(), cache);
            if (handler != null) {
                return new ResolutionResult(handler, t);
            }
        }

        return null;
    }

    private HandlerMethod findHandlerForType(Class<? extends Throwable> type, Map<Class<? extends Throwable>, HandlerMethod> cache) {
        // Walk hierarchy: exact match first, then superclass walk
        Class<?> current = type;
        while (current != null && Throwable.class.isAssignableFrom(current)) {
            @SuppressWarnings("unchecked")
            HandlerMethod handler = cache.get((Class<? extends Throwable>) current);
            if (handler != null) {
                return handler;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Map<Class<? extends Throwable>, HandlerMethod> getCache() {
        if (handlerCache == null) {
            synchronized (this) {
                if (handlerCache == null) {
                    handlerCache = buildHandlerCache();
                }
            }
        }
        return handlerCache;
    }

    private Map<Class<? extends Throwable>, HandlerMethod> buildHandlerCache() {
        Map<Class<? extends Throwable>, HandlerMethod> cache = new HashMap<>();

        for (Object advice : controllerAdvices) {
            Class<?> adviceClass = advice.getClass();
            if (!adviceClass.isAnnotationPresent(SpeedyControllerAdvice.class)) {
                continue;
            }

            for (Method method : adviceClass.getDeclaredMethods()) {
                SpeedyExceptionHandler annotation = method.getAnnotation(SpeedyExceptionHandler.class);
                if (annotation == null) {
                    continue;
                }

                Class<? extends Throwable>[] exceptionTypes = annotation.value();
                int httpStatus = annotation.status();

                boolean acceptsException = method.getParameterCount() == 1
                        && Throwable.class.isAssignableFrom(method.getParameterTypes()[0]);

                for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                    HandlerMethod handlerMethod = new HandlerMethod(
                            exceptionType, httpStatus, method, advice, acceptsException);
                    cache.put(exceptionType, handlerMethod);
                }
            }
        }

        return cache;
    }

    private String invokeHandler(HandlerMethod handler, Throwable throwable) {
        try {
            if (handler.acceptsException) {
                Object result = handler.method.invoke(handler.adviceInstance, throwable);
                return result != null ? result.toString() : null;
            } else {
                Object result = handler.method.invoke(handler.adviceInstance);
                return result != null ? result.toString() : null;
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
            LOGGER.warn("Failed to invoke exception handler {}: {}", handler.method.getName(), cause.getMessage());
            return null;
        }
    }

    static class ResolutionResult {
        final HandlerMethod handler;
        final Throwable matchedThrowable;

        ResolutionResult(HandlerMethod handler, Throwable matchedThrowable) {
            this.handler = handler;
            this.matchedThrowable = matchedThrowable;
        }
    }

    static class HandlerMethod {
        final Class<? extends Throwable> exceptionType;
        final int httpStatus;
        final Method method;
        final Object adviceInstance;
        final boolean acceptsException;

        HandlerMethod(Class<? extends Throwable> exceptionType, int httpStatus, Method method, Object adviceInstance, boolean acceptsException) {
            this.exceptionType = exceptionType;
            this.httpStatus = httpStatus;
            this.method = method;
            this.adviceInstance = adviceInstance;
            this.acceptsException = acceptsException;
        }
    }
}
