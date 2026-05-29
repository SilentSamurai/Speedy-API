package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;
import com.github.silent.samurai.speedy.client.transport.SpeedyTransport;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Speedy facade.
 *
 * <pre>{@code
 * // Full CRUD cycle in under 10 lines (static imports excluded):
 * Speedy speedy = Speedy.connect("http://localhost:8080");
 * speedy.create("User").field("name", "Alice").field("email", "alice@example.com").execute();
 * User u = speedy.get("User").key("id", 1).execute().first(User.class);
 * speedy.update("User").key("id", 1).field("name", "Bob").execute();
 * speedy.delete("User").key("id", 1).execute();
 * }</pre>
 */
class SpeedyTest {

    @Test
    void connectShouldCreateInstanceWithDefaults() {
        Speedy speedy = Speedy.connect("http://localhost:8080");
        assertNotNull(speedy);
    }

    @Test
    void builderShouldConfigureBaseUrl() {
        Speedy speedy = Speedy.builder()
                .baseUrl("http://api.example.com")
                .build();
        assertNotNull(speedy);
    }

    @Test
    void builderShouldRequireBaseUrl() {
        assertThrows(IllegalStateException.class, () -> Speedy.builder().build());
    }

    @Test
    void builderShouldAcceptCustomTransport() {
        AtomicReference<SpeedyRequest> captured = new AtomicReference<>();
        SpeedyTransport fake = request -> {
            captured.set(request);
            return new SpeedyRawResponse(200, Collections.emptyMap(),
                    "{\"payload\":[],\"pageIndex\":0,\"pageSize\":10}");
        };

        Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost:8080")
                .transport(fake)
                .build();

        speedy.create("User").field("name", "Test").execute();
        assertNotNull(captured.get());
        assertEquals("POST", captured.get().method());
        assertTrue(captured.get().url().contains("$create"));
    }

    @Test
    void interceptorShouldModifyRequests() {
        List<String> headerValues = new CopyOnWriteArrayList<>();
        SpeedyTransport fake = request -> {
            headerValues.addAll(request.headers().getOrDefault("X-Custom", List.of()));
            return new SpeedyRawResponse(200, Collections.emptyMap(),
                    "{\"payload\":[],\"pageIndex\":0,\"pageSize\":10}");
        };

        Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost:8080")
                .transport(fake)
                .interceptor(req -> req.withHeader("X-Custom", "test-value"))
                .build();

        speedy.get("User").execute();
        assertTrue(headerValues.contains("test-value"));
    }

    @Test
    void interceptorChainingShouldExecuteInOrder() {
        List<String> calls = new CopyOnWriteArrayList<>();
        SpeedyTransport fake = request -> {
            calls.add("transport");
            return new SpeedyRawResponse(200, Collections.emptyMap(),
                    "{\"payload\":[],\"pageIndex\":0,\"pageSize\":10}");
        };

        Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost:8080")
                .transport(fake)
                .interceptor(req -> {
                    calls.add("first");
                    return req;
                })
                .interceptor(req -> {
                    calls.add("second");
                    return req;
                })
                .build();

        speedy.get("User").execute();
        assertEquals(List.of("first", "second", "transport"), calls);
    }

    @Test
    void apiPathShouldBeConfigurable() {
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        SpeedyTransport fake = request -> {
            capturedUrl.set(request.url());
            return new SpeedyRawResponse(200, Collections.emptyMap(),
                    "{\"payload\":[],\"pageIndex\":0,\"pageSize\":10}");
        };

        Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost:8080")
                .apiPath("/custom/api/")
                .transport(fake)
                .build();

        speedy.get("User").execute();
        assertTrue(capturedUrl.get().contains("/custom/api/"));
    }

    @Test
    void metadataShouldFetchMetadataEndpoint() {
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        SpeedyTransport fake = request -> {
            capturedUrl.set(request.url());
            return new SpeedyRawResponse(200, Collections.emptyMap(),
                    "{\"entities\":[]}");
        };

        Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost:8080")
                .transport(fake)
                .build();

        JsonNode metadata = speedy.metadata();
        assertTrue(capturedUrl.get().endsWith("/$metadata"));
        assertNotNull(metadata);
        assertTrue(metadata.has("entities"));
    }

    @Test
    void customObjectMapperShouldBeUsed() {
        ObjectMapper customMapper = new ObjectMapper();
        customMapper.registerModule(new SimpleModule());   // just verify no errors

        Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost:8080")
                .objectMapper(customMapper)
                .build();

        assertNotNull(speedy);
    }
}
