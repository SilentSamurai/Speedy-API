package com.github.silent.samurai.exceptions;

import javax.servlet.http.HttpServletResponse;

public class BadRequestException extends SpeedyHttpException {

    public BadRequestException() {
        super(HttpServletResponse.SC_BAD_REQUEST, "");
    }

    public BadRequestException(String message) {
        super(HttpServletResponse.SC_BAD_REQUEST, message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(HttpServletResponse.SC_BAD_REQUEST, message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(HttpServletResponse.SC_BAD_REQUEST, cause);
    }

    public BadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(HttpServletResponse.SC_BAD_REQUEST, message, cause, enableSuppression, writableStackTrace);
    }
}
