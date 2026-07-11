package com.github.silent.samurai.speedy.client.exception;

/**
 * Base unchecked exception for all Speedy API client errors.
 *
 * <p>Carries HTTP status code, server error message, timestamp, and raw response body
 * for debugging. Subclasses provide fine-grained error handling by HTTP status category.
 */
public class SpeedyException extends RuntimeException {

    private final int statusCode;
    private final String serverMessage;
    private final String timestamp;
    private final String responseBody;

    public SpeedyException(int statusCode, String serverMessage, String timestamp, String responseBody) {
        super(buildMessage(statusCode, serverMessage));
        this.statusCode = statusCode;
        this.serverMessage = serverMessage;
        this.timestamp = timestamp;
        this.responseBody = responseBody;
    }

    public SpeedyException(String message) {
        super(message);
        this.statusCode = 0;
        this.serverMessage = message;
        this.timestamp = null;
        this.responseBody = null;
    }

    public SpeedyException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.serverMessage = message;
        this.timestamp = null;
        this.responseBody = null;
    }

    private static String buildMessage(int statusCode, String serverMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Speedy API error [").append(statusCode).append("]");
        if (serverMessage != null && !serverMessage.isEmpty()) {
            sb.append(": ").append(serverMessage);
        }
        return sb.toString();
    }

    public int statusCode() {
        return statusCode;
    }

    public String serverMessage() {
        return serverMessage;
    }

    public String timestamp() {
        return timestamp;
    }

    public String responseBody() {
        return responseBody;
    }
}
