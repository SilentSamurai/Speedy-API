package com.github.silent.samurai.speedy.exceptions;

import jakarta.servlet.http.HttpServletResponse;

public class NotFoundException extends SpeedyHttpException {

    public NotFoundException() {
        super(HttpServletResponse.SC_NOT_FOUND, "");
    }

    public NotFoundException(String message) {
        super(HttpServletResponse.SC_NOT_FOUND, message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(HttpServletResponse.SC_NOT_FOUND, message, cause);
    }

    public NotFoundException(Throwable cause) {
        super(HttpServletResponse.SC_NOT_FOUND, cause);
    }

    public NotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(HttpServletResponse.SC_NOT_FOUND, message, cause, enableSuppression, writableStackTrace);
    }
}
