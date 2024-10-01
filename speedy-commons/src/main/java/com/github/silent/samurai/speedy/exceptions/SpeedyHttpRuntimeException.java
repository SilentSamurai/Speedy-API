package com.github.silent.samurai.speedy.exceptions;

public class SpeedyHttpRuntimeException extends RuntimeException {

    private final Integer status;

    public SpeedyHttpRuntimeException(Integer status, String message) {
        super(message);
        this.status = status;
    }

    public SpeedyHttpRuntimeException(Integer status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public SpeedyHttpRuntimeException(Integer status, Throwable cause) {
        super(cause);
        this.status = status;
    }

    public SpeedyHttpRuntimeException(Integer status, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }

}
