package com.github.silent.samurai.speedy.exceptions;

public class InternalServerError extends SpeedyHttpException {

    public InternalServerError() {
        super(500, "");
    }

    public InternalServerError(String message) {
        super(500, message);
    }

    public InternalServerError(String message, Throwable cause) {
        super(500, message, cause);
    }

    public InternalServerError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(500, message, cause, enableSuppression, writableStackTrace);
    }
}
