package com.github.silent.samurai.speedy.exceptions;

public class SpeedyHttpException extends Exception {

    private final Integer status;

    public SpeedyHttpException(Integer status, String message) {
        super(message);
        this.status = status;
    }

    public SpeedyHttpException(Integer status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public SpeedyHttpException(Integer status, Throwable cause) {
        super(cause);
        this.status = status;
    }

    public SpeedyHttpException(Integer status, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }

}
