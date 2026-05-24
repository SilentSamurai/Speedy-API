package com.github.silent.samurai.speedy.client.exception;

/**
 * Thrown when a network-level failure prevents communication with the Speedy API.
 * Wraps {@link java.io.IOException} from the transport layer (connection refused,
 * timeout, DNS resolution failure, SSL errors).
 */
public class SpeedyConnectionException extends SpeedyException {

    public SpeedyConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpeedyConnectionException(String message) {
        super(message);
    }
}
