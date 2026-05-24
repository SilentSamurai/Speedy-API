package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.silent.samurai.speedy.client.exception.SpeedyBadRequestException;
import com.github.silent.samurai.speedy.client.exception.SpeedyConnectionException;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;
import com.github.silent.samurai.speedy.client.transport.SpeedyTransport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SpeedyErrorHandlingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldThrowSpeedyConnectionExceptionForConnectionRefused() {
        SpeedyTransport failingTransport = request -> {
            throw new IOException("Connection refused");
        };

        Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost:1")
                .transport(failingTransport)
                .build();

        assertThrows(SpeedyConnectionException.class, () -> speedy.get("User").execute());
    }

    @Test
    void shouldThrowSpeedyBadRequestExceptionFor400() {
        SpeedyTransport badRequestTransport = request ->
                new SpeedyRawResponse(400, Map.of(),
                        "{\"status\":400,\"message\":\"Validation failed\",\"timestamp\":\"2024-01-01T00:00:00Z\"}");

        Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost")
                .transport(badRequestTransport)
                .build();

        SpeedyBadRequestException ex = assertThrows(SpeedyBadRequestException.class,
                () -> speedy.create("User").field("name", "").execute());
        assertEquals(400, ex.statusCode());
        assertEquals("Validation failed", ex.serverMessage());
        assertNotNull(ex.timestamp());
    }

    @Test
    void shouldThrowSpeedyConnectionExceptionForTimeout() {
        SpeedyTransport timeoutTransport = request -> {
            throw new IOException("Read timed out");
        };

        Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost")
                .transport(timeoutTransport)
                .build();

        assertThrows(SpeedyConnectionException.class, () -> speedy.query("User").execute());
    }

    @Test
    void bulkCreateShouldPropagateErrors() {
        SpeedyTransport errorTransport = request ->
                new SpeedyRawResponse(400, Map.of(), "{\"status\":400,\"message\":\"Bad\"}");

        Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost")
                .transport(errorTransport)
                .build();

        ArrayNode entities = mapper.createArrayNode();
        entities.addObject().put("name", "Test");
        assertThrows(SpeedyBadRequestException.class, () ->
                speedy.createMany("User", List.of(mapper.createObjectNode())));
    }
}
