package com.github.silent.samurai.exceptions;

import org.springframework.http.HttpStatus;

public class BadRequestException extends SpeedyHttpException {

    public BadRequestException() {
        super(HttpStatus.BAD_REQUEST, "");
    }

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(HttpStatus.BAD_REQUEST, cause);
    }

    public BadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(HttpStatus.BAD_REQUEST, message, cause, enableSuppression, writableStackTrace);
    }
}
