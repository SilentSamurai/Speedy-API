package com.github.silent.samurai.speedy.exceptions;

public class BadRequestException extends SpeedyHttpException {

    public BadRequestException() {
        super(400, "");
    }

    public BadRequestException(String message) {
        super(400, message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(400, message, cause);
    }

    public BadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(400, message, cause, enableSuppression, writableStackTrace);
    }
}
