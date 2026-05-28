package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.annotations.SpeedyControllerAdvice;
import com.github.silent.samurai.speedy.annotations.SpeedyExceptionHandler;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyExceptionMapper;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ExceptionMapperTest {

    private List<Object> controllerAdvices;

    @BeforeEach
    void setUp() {
        controllerAdvices = new ArrayList<>();
    }

    @SpeedyControllerAdvice
    static class TestAdvice {

        @SpeedyExceptionHandler(value = NotFoundException.class, status = 404)
        public String handleNotFound(NotFoundException e) {
            return "Entity not found: " + e.getMessage();
        }

        @SpeedyExceptionHandler(value = IllegalArgumentException.class, status = 400)
        public String handleIllegalArg(IllegalArgumentException e) {
            return "Invalid argument: " + e.getMessage();
        }

        @SpeedyExceptionHandler(value = RuntimeException.class, status = 500)
        public String handleRuntimeException() {
            return "Unexpected error";
        }
    }

    static class CustomException extends RuntimeException {
        CustomException(String message) {
            super(message);
        }
    }

    static class CustomCheckedException extends Exception {
        CustomCheckedException(String message) {
            super(message);
        }
    }

    // --- AdviceExceptionMapper tests ---

    @Test
    void adviceMapperReturnsMinusOneWhenNoAdvice() {
        AdviceExceptionMapper mapper = new AdviceExceptionMapper(controllerAdvices);
        Assertions.assertEquals(-1, mapper.getStatus(new RuntimeException("test")));
    }

    @Test
    void adviceMapperReturnsNullMessageWhenNoAdvice() {
        AdviceExceptionMapper mapper = new AdviceExceptionMapper(controllerAdvices);
        Assertions.assertNull(mapper.getMessage(new RuntimeException("test")));
    }

    @Test
    void adviceMapperFindsExactExceptionMatch() {
        controllerAdvices.add(new TestAdvice());
        AdviceExceptionMapper mapper = new AdviceExceptionMapper(controllerAdvices);

        int status = mapper.getStatus(new IllegalArgumentException("bad arg"));
        Assertions.assertEquals(400, status);
    }

    @Test
    void adviceMapperReturnsMessageFromHandlerWithParameter() {
        controllerAdvices.add(new TestAdvice());
        AdviceExceptionMapper mapper = new AdviceExceptionMapper(controllerAdvices);

        String message = mapper.getMessage(new NotFoundException("Product not found: 123"));
        Assertions.assertEquals("Entity not found: Product not found: 123", message);
    }

    @Test
    void adviceMapperReturnsMessageFromHandlerWithoutParameter() {
        controllerAdvices.add(new TestAdvice());
        AdviceExceptionMapper mapper = new AdviceExceptionMapper(controllerAdvices);

        String message = mapper.getMessage(new RuntimeException("boom"));
        Assertions.assertEquals("Unexpected error", message);
    }

    @Test
    void adviceMapperWalksSuperclassHierarchy() {
        controllerAdvices.add(new TestAdvice());
        AdviceExceptionMapper mapper = new AdviceExceptionMapper(controllerAdvices);

        int status = mapper.getStatus(new CustomException("test"));
        Assertions.assertEquals(500, status);
    }

    @Test
    void adviceMapperWalksCauseChain() {
        controllerAdvices.add(new TestAdvice());
        AdviceExceptionMapper mapper = new AdviceExceptionMapper(controllerAdvices);

        CustomCheckedException exception = new CustomCheckedException("wrapped");
        IllegalArgumentException cause = new IllegalArgumentException("root cause");
        exception.initCause(cause);

        int status = mapper.getStatus(exception);
        Assertions.assertEquals(400, status);
    }

    @Test
    void adviceMapperRootCauseHasPriority() {
        controllerAdvices.add(new TestAdvice());
        AdviceExceptionMapper mapper = new AdviceExceptionMapper(controllerAdvices);

        Exception exception = new Exception("generic wrapper",
                new IllegalArgumentException("root cause"));

        int status = mapper.getStatus(exception);
        Assertions.assertEquals(400, status);
    }

    // --- DefaultExceptionMapper tests ---

    @Test
    void defaultMapperHandlesSpeedyHttpException() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        NotFoundException e = new NotFoundException("Item 42");

        Assertions.assertEquals(HttpServletResponse.SC_NOT_FOUND, mapper.getStatus(e));
    }

    @Test
    void defaultMapperHandlesSpeedyHttpRuntimeException() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));

        int status = mapper.getStatus(new com.github.silent.samurai.speedy.exceptions.ConversionException("bad type"));
        Assertions.assertEquals(400, status);
    }

    @Test
    void defaultMapperHandlesIllegalArgumentException() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));

        int status = mapper.getStatus(new IllegalArgumentException("invalid"));
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, status);
    }

    @Test
    void defaultMapperHandlesPersistenceExceptionWithConstraintViolation() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        ConstraintViolationException cause = new ConstraintViolationException("constraint violated", null, "unique_email");
        PersistenceException e = new PersistenceException("persistence error", cause);

        int status = mapper.getStatus(e);
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, status);
    }

    @Test
    void defaultMapperHandlesPersistenceExceptionWithDataException() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        DataException cause = new DataException("data error", null);
        PersistenceException e = new PersistenceException("persistence error", cause);

        int status = mapper.getStatus(e);
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, status);
    }

    @Test
    void defaultMapperHandlesJsonProcessingException() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        com.fasterxml.jackson.core.JsonProcessingException e = new com.fasterxml.jackson.core.JsonProcessingException("invalid json") {};

        int status = mapper.getStatus(e);
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, status);
    }

    @Test
    void defaultMapperReturns500ForUnknownException() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));

        int status = mapper.getStatus(new RuntimeException("unknown"));
        Assertions.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, status);
    }

    @Test
    void defaultMapperExposesMessageFor4xx() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        IllegalArgumentException e = new IllegalArgumentException("field 'email' is invalid");

        String message = mapper.getMessage(e);
        Assertions.assertEquals("field 'email' is invalid", message);
    }

    @Test
    void defaultMapperMasksMessageFor5xx() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        RuntimeException e = new RuntimeException("sensitive database details");

        String message = mapper.getMessage(e);
        Assertions.assertEquals("Internal Server Error", message);
    }

    @Test
    void customAdviceTakesPriorityOverDefaults() {
        controllerAdvices.add(new TestAdvice());
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        IllegalArgumentException e = new IllegalArgumentException("test");

        String message = mapper.getMessage(e);
        Assertions.assertEquals("Invalid argument: test", message);
    }

    @Test
    void customAdvice5xxReturnsMaskedMessage() {
        @SpeedyControllerAdvice
        class Advice5xx {
            @SpeedyExceptionHandler(value = CustomCheckedException.class, status = 502)
            public String handleCustom() {
                return "Custom handler details";
            }
        }
        controllerAdvices.add(new Advice5xx());
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        CustomCheckedException e = new CustomCheckedException("test");

        String message = mapper.getMessage(e);
        Assertions.assertEquals("Internal Server Error", message);
    }

    @Test
    void defaultMapperReturnsClassNameWhenMessageIsNull() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        RuntimeException e = new RuntimeException();

        String message = mapper.getMessage(e);
        Assertions.assertEquals("Internal Server Error", message);
    }

    @Test
    void ispeedyExceptionMapperInterfaceContract() {
        ISpeedyExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));

        Assertions.assertEquals(400, mapper.getStatus(new IllegalArgumentException("test")));
        Assertions.assertEquals("test", mapper.getMessage(new IllegalArgumentException("test")));
    }

    // Regression: #82 — Runtime exceptions returning 500
    @Test
    void conversionExceptionReturns400() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        com.github.silent.samurai.speedy.exceptions.ConversionException e =
                new com.github.silent.samurai.speedy.exceptions.ConversionException("Cannot convert type");

        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, mapper.getStatus(e));
        Assertions.assertEquals("Cannot convert type", mapper.getMessage(e));
    }

    @Test
    void badRequestExceptionReturns400() {
        DefaultExceptionMapper mapper = new DefaultExceptionMapper(new AdviceExceptionMapper(controllerAdvices));
        BadRequestException e = new BadRequestException("Validation failed");

        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, mapper.getStatus(e));
        Assertions.assertEquals("Validation failed", mapper.getMessage(e));
    }
}
