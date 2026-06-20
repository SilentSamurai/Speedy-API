package org.hibernate.exception;

public class ConstraintViolationException extends RuntimeException {

    public ConstraintViolationException(String message, Throwable cause, String constraintName) {
        super(message, cause);
    }
}
