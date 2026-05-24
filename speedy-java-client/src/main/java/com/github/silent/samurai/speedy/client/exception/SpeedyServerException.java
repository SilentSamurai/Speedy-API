package com.github.silent.samurai.speedy.client.exception;

/**
 * Thrown when the Speedy API returns a 5xx Server Error response.
 * Indicates an internal server error.
 */
public class SpeedyServerException extends SpeedyException {

    public SpeedyServerException(int statusCode, String serverMessage, String timestamp, String responseBody) {
        super(statusCode, serverMessage, timestamp, responseBody);
    }
}
