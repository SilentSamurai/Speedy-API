package com.github.silent.samurai.speedy.exceptions;

public class PayloadTooLargeException extends SpeedyHttpException {

    public PayloadTooLargeException(String message) {
        super(413, message);
    }

    public PayloadTooLargeException(String message, Throwable cause) {
        super(413, message, cause);
    }

    public PayloadTooLargeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(413, message, cause, enableSuppression, writableStackTrace);
    }

}
