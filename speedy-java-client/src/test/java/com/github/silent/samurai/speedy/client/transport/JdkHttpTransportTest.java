package com.github.silent.samurai.speedy.client.transport;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JdkHttpTransportTest {

    @Test
    void transportConstructorShouldNotThrow() {
        assertDoesNotThrow((org.junit.jupiter.api.function.Executable) JdkHttpTransport::new);
    }

    @Test
    void customHttpClientShouldBeAccepted() {
        java.net.http.HttpClient custom = java.net.http.HttpClient.newHttpClient();
        JdkHttpTransport transport = new JdkHttpTransport(custom);
        assertNotNull(transport);
    }

    @Test
    void requestShouldPreserveHeaders() {
        SpeedyRequest request = new SpeedyRequest("GET", "http://localhost", null, null);
        assertEquals(Collections.emptyMap(), request.headers());
    }

    @Test
    void withHeaderShouldAddHeader() {
        SpeedyRequest request = new SpeedyRequest("GET", "http://localhost", new HashMap<>(), null);
        SpeedyRequest modified = request.withHeader("Authorization", "Bearer token");

        assertTrue(modified.headers().containsKey("Authorization"));
        assertEquals(List.of("Bearer token"), modified.headers().get("Authorization"));
        assertTrue(request.headers().isEmpty());
    }

    @Test
    void withHeadersShouldAddMultipleHeaders() {
        SpeedyRequest request = new SpeedyRequest("GET", "http://localhost", new HashMap<>(), null);
        Map<String, String> headers = Map.of("X-Trace", "trace-id", "X-Tenant", "tenant-1");
        SpeedyRequest modified = request.withHeaders(headers);

        assertTrue(modified.headers().containsKey("X-Trace"));
        assertTrue(modified.headers().containsKey("X-Tenant"));
    }

    @Test
    void rawResponseShouldPreserveStatusCode() {
        SpeedyRawResponse response = new SpeedyRawResponse(201, Map.of(), "body");
        assertEquals(201, response.statusCode());
        assertEquals("body", response.body());
        assertTrue(response.is2xx());
    }

    @Test
    void rawResponseShouldDetectStatusCategories() {
        assertTrue(new SpeedyRawResponse(200, Map.of(), null).is2xx());
        assertTrue(new SpeedyRawResponse(400, Map.of(), null).is4xx());
        assertTrue(new SpeedyRawResponse(500, Map.of(), null).is5xx());
        assertFalse(new SpeedyRawResponse(201, Map.of(), null).is4xx());
    }

    @Test
    void rawResponseShouldHandleNullHeaders() {
        SpeedyRawResponse response = new SpeedyRawResponse(200, null, null);
        assertTrue(response.headers().isEmpty());
    }
}
