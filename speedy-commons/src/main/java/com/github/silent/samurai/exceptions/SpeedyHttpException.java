package com.github.silent.samurai.exceptions;

import org.springframework.http.HttpStatus;

public class SpeedyHttpException extends Exception {

    private final HttpStatus status;

    public SpeedyHttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public SpeedyHttpException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public SpeedyHttpException(HttpStatus status, Throwable cause) {
        super(cause);
        this.status = status;
    }

    public SpeedyHttpException(HttpStatus status, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.status = status;
    }
}
