package com.github.silent.samurai.speedy.exceptions;

public class NotFoundException extends SpeedyHttpException {

    public NotFoundException() {
        super(404, "");
    }

    public NotFoundException(String message) {
        super(404, message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(404, message, cause);
    }

    public NotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(404, message, cause, enableSuppression, writableStackTrace);
    }
}
