package com.github.silent.samurai.speedy.exceptions;

import jakarta.servlet.http.HttpServletResponse;

public class PayloadTooLargeException extends SpeedyHttpException {

    public PayloadTooLargeException(String message) {
        super(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, message);
    }

    public PayloadTooLargeException(String message, Throwable cause) {
        super(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, message, cause);
    }

    public PayloadTooLargeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, message, cause, enableSuppression, writableStackTrace);
    }

}
