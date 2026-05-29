package com.github.silent.samurai.speedy.exceptions;

public class TestBusinessException extends RuntimeException {

    public TestBusinessException(String message) {
        super(message);
    }

    public TestBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
