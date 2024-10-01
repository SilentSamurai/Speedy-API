package com.github.silent.samurai.speedy.exceptions;

import jakarta.servlet.http.HttpServletResponse;

public class ConversionException extends SpeedyHttpRuntimeException {

    public ConversionException() {
        super(HttpServletResponse.SC_BAD_REQUEST, "");
    }

    public ConversionException(String message) {
        super(HttpServletResponse.SC_BAD_REQUEST, message);
    }

    public ConversionException(String message, Throwable cause) {
        super(HttpServletResponse.SC_BAD_REQUEST, message, cause);
    }

    public ConversionException(Throwable cause) {
        super(HttpServletResponse.SC_BAD_REQUEST, cause);
    }

    public ConversionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(HttpServletResponse.SC_BAD_REQUEST, message, cause, enableSuppression, writableStackTrace);
    }
}
