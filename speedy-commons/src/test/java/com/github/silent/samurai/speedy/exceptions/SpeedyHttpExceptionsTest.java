package com.github.silent.samurai.speedy.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeedyHttpExceptionsTest {

    @Test
    void forbiddenException_carries403Status() {
        ForbiddenException ex = new ForbiddenException("denied");
        assertEquals(403, ex.getStatus());
        assertEquals("denied", ex.getMessage());
    }

    @Test
    void forbiddenException_preservesCause() {
        Throwable cause = new RuntimeException("root");
        ForbiddenException ex = new ForbiddenException("denied", cause);
        assertEquals(403, ex.getStatus());
        assertSame(cause, ex.getCause());
    }

    @Test
    void notImplementedException_carries501Status() {
        NotImplementedException ex = new NotImplementedException("not supported");
        assertEquals(501, ex.getStatus());
        assertEquals("not supported", ex.getMessage());
    }

    @Test
    void notImplementedException_preservesCause() {
        Throwable cause = new RuntimeException("root");
        NotImplementedException ex = new NotImplementedException("not supported", cause);
        assertEquals(501, ex.getStatus());
        assertSame(cause, ex.getCause());
    }

    @Test
    void exceptions_arePartOfSpeedyHttpExceptionHierarchy() {
        assertTrue(new ForbiddenException("x") instanceof SpeedyHttpException);
        assertTrue(new NotImplementedException("x") instanceof SpeedyHttpException);
    }
}
