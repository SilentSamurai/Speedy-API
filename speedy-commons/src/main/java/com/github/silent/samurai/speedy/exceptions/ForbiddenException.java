package com.github.silent.samurai.speedy.exceptions;

public class ForbiddenException extends SpeedyHttpException {

    public ForbiddenException() {
        super(403, "");
    }

    public ForbiddenException(String message) {
        super(403, message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(403, message, cause);
    }

    public ForbiddenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(403, message, cause, enableSuppression, writableStackTrace);
    }
}
