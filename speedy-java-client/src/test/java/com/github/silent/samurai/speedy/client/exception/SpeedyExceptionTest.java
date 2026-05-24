package com.github.silent.samurai.speedy.client.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeedyExceptionTest {

    @Test
    void badRequestShouldHaveStatusCode400() {
        SpeedyBadRequestException ex = new SpeedyBadRequestException("Bad input", "2024-01-01T00:00:00Z", "{}");
        assertEquals(400, ex.statusCode());
        assertEquals("Bad input", ex.serverMessage());
        assertEquals("2024-01-01T00:00:00Z", ex.timestamp());
        assertEquals("{}", ex.responseBody());
    }

    @Test
    void notFoundShouldHaveStatusCode404() {
        SpeedyNotFoundException ex = new SpeedyNotFoundException(null, null, null);
        assertEquals(404, ex.statusCode());
    }

    @Test
    void serverExceptionShouldPreserveStatusCode() {
        SpeedyServerException ex = new SpeedyServerException(502, "Bad gateway", null, null);
        assertEquals(502, ex.statusCode());
    }

    @Test
    void connectionExceptionShouldHaveMessage() {
        SpeedyConnectionException ex = new SpeedyConnectionException("Connection refused", new RuntimeException());
        assertEquals("Connection refused", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    @Test
    void deserializationExceptionShouldHaveCause() {
        SpeedyDeserializationException ex = new SpeedyDeserializationException("Parse error", new RuntimeException());
        assertEquals("Parse error", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    @Test
    void allExceptionsShouldBeUnchecked() {
        assertTrue(SpeedyBadRequestException.class.getSuperclass() == SpeedyException.class);
        assertTrue(SpeedyNotFoundException.class.getSuperclass() == SpeedyException.class);
        assertTrue(SpeedyServerException.class.getSuperclass() == SpeedyException.class);
        assertTrue(SpeedyConnectionException.class.getSuperclass() == SpeedyException.class);
        assertTrue(SpeedyDeserializationException.class.getSuperclass() == SpeedyException.class);
        assertTrue(RuntimeException.class.isAssignableFrom(SpeedyException.class));
    }

    @Test
    void baseExceptionShouldContainStatusCodeInMessage() {
        SpeedyException ex = new SpeedyException(400, "Bad request", null, null);
        assertTrue(ex.getMessage().contains("400"));
        assertTrue(ex.getMessage().contains("Bad request"));
    }

    @Test
    void exceptionWithNullFieldsShouldNotThrow() {
        SpeedyBadRequestException ex = new SpeedyBadRequestException(null, null, null);
        assertEquals(400, ex.statusCode());
        assertNull(ex.serverMessage());
        assertNull(ex.timestamp());
        assertNull(ex.responseBody());
    }
}
