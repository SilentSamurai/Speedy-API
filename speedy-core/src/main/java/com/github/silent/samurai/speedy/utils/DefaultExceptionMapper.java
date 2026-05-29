package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyExceptionMapper;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;

public class DefaultExceptionMapper implements ISpeedyExceptionMapper {

    private final AdviceExceptionMapper adviceExceptionMapper;

    public DefaultExceptionMapper(AdviceExceptionMapper adviceExceptionMapper) {
        this.adviceExceptionMapper = adviceExceptionMapper;
    }

    @Override
    public int getStatus(Throwable throwable) {
        // 1. Custom advice handlers (priority)
        int adviceStatus = adviceExceptionMapper.getStatus(throwable);
        if (adviceStatus > 0) {
            return adviceStatus;
        }

        // 2. SpeedyHttpException hierarchy
        if (throwable instanceof SpeedyHttpException) {
            return ((SpeedyHttpException) throwable).getStatus();
        }
        if (throwable instanceof SpeedyHttpRuntimeException) {
            return ((SpeedyHttpRuntimeException) throwable).getStatus();
        }

        // 3. PersistenceException — unwrap cause
        if (throwable instanceof PersistenceException && throwable.getCause() != null) {
            Throwable cause = throwable.getCause();
            if (cause instanceof ConstraintViolationException || cause instanceof DataException) {
                return HttpServletResponse.SC_BAD_REQUEST;
            }
        }

        // 4. Jackson parsing errors
        if (hasClassInHierarchy(throwable, "com.fasterxml.jackson.core.JsonProcessingException")) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }

        // 5. ConversionException
        if (hasClassInHierarchy(throwable, "com.github.silent.samurai.speedy.exceptions.ConversionException")) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }

        // 6. IllegalArgumentException
        if (throwable instanceof IllegalArgumentException) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }

        // 7. Fallback
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    @Override
    public String getMessage(Throwable throwable) {
        // 1. Custom advice handlers (priority)
        String adviceMessage = adviceExceptionMapper.getMessage(throwable);
        if (adviceMessage != null) {
            // Respect custom messages even for 500+ errors
            return adviceMessage;
        }

        // 2. Use throwable's message for non-500 errors
        String message = throwable.getMessage();
        if (message == null || message.isEmpty()) {
            message = throwable.getClass().getSimpleName();
        }

        // 3. Fallback masking for 5xx
        int status = getStatus(throwable);
        if (status >= 500) {
            return "Internal Server Error";
        }

        return message;
    }

    private static boolean hasClassInHierarchy(Throwable throwable, String className) {
        Class<?> current = throwable.getClass();
        while (current != null) {
            if (current.getName().equals(className)) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
