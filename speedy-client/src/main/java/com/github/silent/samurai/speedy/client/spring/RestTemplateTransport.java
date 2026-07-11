package com.github.silent.samurai.speedy.client.spring;

import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;
import com.github.silent.samurai.speedy.client.transport.SpeedyTransport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring {@link RestTemplate} adapter implementing the {@link SpeedyTransport} SPI.
 * Catches Spring's HTTP error exceptions internally and returns responses with status intact.
 *
 * <p>Requires {@code spring-web} on the classpath (optional dependency).
 */
public class RestTemplateTransport implements SpeedyTransport {

    private static final String DEFAULT_MEDIA_TYPE = "application/json;charset=UTF-8";

    private final RestTemplate restTemplate;

    public RestTemplateTransport() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    public RestTemplateTransport(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public SpeedyRawResponse send(SpeedyRequest request) throws IOException {
        try {
            HttpMethod method = HttpMethod.valueOf(request.method());
            HttpHeaders headers = new HttpHeaders();
            if (!hasHeader(request, "Accept")) {
                headers.set("Accept", DEFAULT_MEDIA_TYPE);
            }
            if (request.body() != null && !request.body().isEmpty() && !hasHeader(request, "Content-Type")) {
                headers.set("Content-Type", DEFAULT_MEDIA_TYPE);
            }
            for (Map.Entry<String, List<String>> entry : request.headers().entrySet()) {
                for (String value : entry.getValue()) {
                    headers.add(entry.getKey(), value);
                }
            }

            HttpEntity<String> entity = request.body() != null
                    ? new HttpEntity<>(request.body(), headers)
                    : new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    request.url(), method, entity, String.class);

            Map<String, List<String>> responseHeaders = new HashMap<>();
            response.getHeaders().forEach((key, values) ->
                    responseHeaders.put(key, new ArrayList<>(values)));

            String body = response.getBody();
            int statusCode = response.getStatusCode().value();

            return new SpeedyRawResponse(statusCode, responseHeaders, body);

        } catch (HttpStatusCodeException e) {
            Map<String, List<String>> responseHeaders = new HashMap<>();
            if (e.getResponseHeaders() != null) {
                e.getResponseHeaders().forEach((key, values) ->
                        responseHeaders.put(key, new ArrayList<>(values)));
            }

            return new SpeedyRawResponse(
                    e.getStatusCode().value(),
                    responseHeaders,
                    e.getResponseBodyAsString());
        }
    }

    private static boolean hasHeader(SpeedyRequest request, String name) {
        return request.headers().keySet().stream().anyMatch(k -> k.equalsIgnoreCase(name));
    }
}
