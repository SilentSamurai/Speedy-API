package com.github.silent.samurai.speedy.exceptions;

import jakarta.servlet.http.HttpServletResponse;

public class InternalServerError extends SpeedyHttpException {

    public InternalServerError() {
        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "");
    }

    public InternalServerError(String message) {
        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

    public InternalServerError(String message, Throwable cause) {
        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message, cause);
    }

//    public InternalServerError(Throwable cause) {
//        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, cause);
//    }

    public InternalServerError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message, cause, enableSuppression, writableStackTrace);
    }
}
