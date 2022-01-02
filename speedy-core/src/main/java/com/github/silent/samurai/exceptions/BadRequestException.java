package com.github.silent.samurai.exceptions;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String toString) {
        super(toString);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

    public BadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BadRequestException() {
    }
}
