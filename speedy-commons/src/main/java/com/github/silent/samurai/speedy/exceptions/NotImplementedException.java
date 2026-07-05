package com.github.silent.samurai.speedy.exceptions;

public class NotImplementedException extends SpeedyHttpException {

    public NotImplementedException() {
        super(501, "");
    }

    public NotImplementedException(String message) {
        super(501, message);
    }

    public NotImplementedException(String message, Throwable cause) {
        super(501, message, cause);
    }

    public NotImplementedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(501, message, cause, enableSuppression, writableStackTrace);
    }
}
