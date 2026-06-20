package com.github.silent.samurai.speedy.exceptions;

public class ConversionException extends SpeedyHttpRuntimeException {

    public ConversionException() {
        super(400, "");
    }

    public ConversionException(String message) {
        super(400, message);
    }

    public ConversionException(String message, Throwable cause) {
        super(400, message, cause);
    }

    public ConversionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(400, message, cause, enableSuppression, writableStackTrace);
    }
}
