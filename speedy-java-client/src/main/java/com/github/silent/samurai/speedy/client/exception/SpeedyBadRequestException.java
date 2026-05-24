package com.github.silent.samurai.speedy.client.exception;

/**
 * Thrown when the Speedy API returns a 400 Bad Request response.
 * Indicates invalid input data or validation failures.
 */
public class SpeedyBadRequestException extends SpeedyException {

    public SpeedyBadRequestException(String serverMessage, String timestamp, String responseBody) {
        super(400, serverMessage, timestamp, responseBody);
    }
}
