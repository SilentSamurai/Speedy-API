package com.github.silent.samurai.speedy.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.internal.PathBuilder;
import com.github.silent.samurai.speedy.client.internal.RequestSender;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ReplaceBuilderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PathBuilder paths = new PathBuilder("http://localhost:8080", "/speedy/v1/");
    private final ResponseParser parser = new ResponseParser(mapper);

    @Test
    void dotNotationFieldShouldAppearInBuildOutput() {
        RequestSender sender = r -> new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        ReplaceBuilder builder = new ReplaceBuilder("User", paths, sender, mapper, parser);
        ObjectNode built = builder.field("profile.bio", "Hello").build();
        assertTrue(built.has("profile"));
        assertEquals("Hello", built.get("profile").get("bio").asText());
    }

    @Test
    void pkShouldBeIncludedInRequestBody() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        RequestSender sender = request -> {
            capturedBody.set(request.body());
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        };

        ReplaceBuilder builder = new ReplaceBuilder("User", paths, sender, mapper, parser);
        builder.key("id", 123).field("name", "Bob").execute();

        assertNotNull(capturedBody.get());
        assertTrue(capturedBody.get().contains("\"id\""));
        assertTrue(capturedBody.get().contains("\"name\""));
    }

    @Test
    void executeShouldUsePutMethod() {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        RequestSender sender = request -> {
            capturedMethod.set(request.method());
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        };

        ReplaceBuilder builder = new ReplaceBuilder("User", paths, sender, mapper, parser);
        builder.key("id", 1).field("name", "Replaced").execute();

        assertEquals("PUT", capturedMethod.get());
    }

    @Test
    void executeShouldReturnResult() {
        RequestSender sender = r -> new SpeedyRawResponse(200, Collections.emptyMap(),
                "{\"payload\":[{\"id\":1,\"name\":\"Replaced\"}],\"pageIndex\":0,\"pageSize\":10}");

        ReplaceBuilder builder = new ReplaceBuilder("User", paths, sender, mapper, parser);
        SpeedyResult result = builder.key("id", 1).field("name", "Replaced").execute();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
