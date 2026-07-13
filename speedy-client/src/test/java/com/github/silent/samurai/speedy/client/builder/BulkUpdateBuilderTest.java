package com.github.silent.samurai.speedy.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.client.Speedy;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyTransport;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BulkUpdateBuilderTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode node(String id, String name) {
        ObjectNode n = mapper.createObjectNode();
        n.put("id", id);
        n.put("name", name);
        return n;
    }

    private Speedy speedyWith(SpeedyTransport transport) {
        return Speedy.builder()
                .baseUrl("http://localhost:8080")
                .transport(transport)
                .build();
    }

    @Test
    void executeShouldUsePatchMethod() {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        Speedy speedy = speedyWith(request -> {
            capturedMethod.set(request.method());
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        });

        speedy.updateMany("User").items(List.of(node("1", "Alice"))).execute();

        assertEquals("PATCH", capturedMethod.get());
    }

    @Test
    void requestBodyShouldBeAJsonArray() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        Speedy speedy = speedyWith(request -> {
            capturedBody.set(request.body());
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        });

        speedy.updateMany("User").items(List.of(node("1", "Alice"), node("2", "Bob"))).execute();

        assertNotNull(capturedBody.get());
        assertTrue(capturedBody.get().trim().startsWith("["));
        assertTrue(capturedBody.get().contains("\"Alice\""));
        assertTrue(capturedBody.get().contains("\"Bob\""));
    }

    @Test
    void urlShouldContainUpdateSuffix() {
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        Speedy speedy = speedyWith(request -> {
            capturedUrl.set(request.url());
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        });

        speedy.updateMany("User").items(List.of(node("1", "Alice"))).execute();

        assertTrue(capturedUrl.get().endsWith(SpeedyEndpoint.UPDATE.path()));
    }

    @Test
    void executeShouldReturnResult() {
        Speedy speedy = speedyWith(request -> new SpeedyRawResponse(200, Collections.emptyMap(),
                "{\"payload\":[{\"id\":1,\"name\":\"Updated\"}],\"pageIndex\":0,\"pageSize\":10}"));

        SpeedyResult result = speedy.updateMany("User").items(List.of(node("1", "Alice"))).execute();

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void listOverloadShouldUsePatchMethodAndSendAllItems() {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        Speedy speedy = speedyWith(request -> {
            capturedMethod.set(request.method());
            capturedBody.set(request.body());
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        });

        speedy.updateMany("User", List.of(node("1", "Alice"), node("2", "Bob")));

        assertEquals("PATCH", capturedMethod.get());
        assertTrue(capturedBody.get().contains("\"Alice\""));
        assertTrue(capturedBody.get().contains("\"Bob\""));
    }

    @Test
    void executeShouldThrowWhenItemsNotSet() {
        Speedy speedy = speedyWith(request ->
                new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}"));

        assertThrows(IllegalStateException.class, () -> speedy.updateMany("User").execute());
    }
}
