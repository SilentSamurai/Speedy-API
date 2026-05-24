package com.github.silent.samurai.speedy.client.exception;

/**
 * Thrown when the Speedy API returns a 404 Not Found response.
 * Indicates the requested entity or endpoint does not exist.
 */
public class SpeedyNotFoundException extends SpeedyException {

    public SpeedyNotFoundException(String serverMessage, String timestamp, String responseBody) {
        super(404, serverMessage, timestamp, responseBody);
    }
}
