package com.github.silent.samurai.speedy.exceptions;

public class PreconditionFailedException extends SpeedyHttpException {

    public PreconditionFailedException() {
        super(412, "");
    }

    public PreconditionFailedException(String message) {
        super(412, message);
    }

    public PreconditionFailedException(String message, Throwable cause) {
        super(412, message, cause);
    }

    public PreconditionFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(412, message, cause, enableSuppression, writableStackTrace);
    }
}
