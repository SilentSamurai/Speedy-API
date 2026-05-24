package com.github.silent.samurai.speedy.client.exception;

/**
 * Thrown when JSON deserialization of the server response body fails.
 * Indicates an unexpected response format or a client-side deserialization bug.
 */
public class SpeedyDeserializationException extends SpeedyException {

    public SpeedyDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpeedyDeserializationException(String message) {
        super(message);
    }
}
