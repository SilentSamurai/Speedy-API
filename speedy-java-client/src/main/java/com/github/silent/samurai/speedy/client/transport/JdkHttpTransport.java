package com.github.silent.samurai.speedy.client.transport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default transport implementation using JDK 11+ {@link java.net.http.HttpClient}.
 * Zero Spring dependency. Thread-safe.
 *
 * <p>Timeouts: 30s connect, 60s request.
 */
public class JdkHttpTransport implements SpeedyTransport {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;

    public JdkHttpTransport() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public JdkHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public SpeedyRawResponse send(SpeedyRequest request) throws IOException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(request.url()))
                    .timeout(REQUEST_TIMEOUT);

            String method = request.method();
            String body = request.body();
            if (body != null && !body.isEmpty()) {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            builder.header("Accept", "application/json;charset=UTF-8");
            if (body != null && !body.isEmpty()) {
                builder.header("Content-Type", "application/json;charset=UTF-8");
            }

            for (Map.Entry<String, List<String>> entry : request.headers().entrySet()) {
                for (String value : entry.getValue()) {
                    builder.header(entry.getKey(), value);
                }
            }

            HttpRequest httpRequest = builder.build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            Map<String, List<String>> responseHeaders = new HashMap<>(response.headers().map());
            return new SpeedyRawResponse(response.statusCode(), responseHeaders, response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }
}
