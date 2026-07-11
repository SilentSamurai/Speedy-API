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

class CreateBuilderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PathBuilder paths = new PathBuilder("http://localhost:8080", "/speedy/v1/");
    private final ResponseParser parser = new ResponseParser(mapper);
    private final RequestSender sender = request -> new SpeedyRawResponse(200, Collections.emptyMap(),
            "{\"payload\":[{\"id\":1,\"name\":\"Test\"}],\"pageIndex\":0,\"pageSize\":10}");

    @Test
    void simpleFieldShouldBeInBuildOutput() {
        CreateBuilder builder = new CreateBuilder("User", paths, sender, mapper, parser);
        ObjectNode built = builder.field("name", "Alice").build();
        assertEquals("Alice", built.get("name").asText());
    }

    @Test
    void dotNotationShouldCreateNestedFields() {
        CreateBuilder builder = new CreateBuilder("User", paths, sender, mapper, parser);
        ObjectNode built = builder.field("address.city", "Seattle").build();
        assertTrue(built.has("address"));
        assertEquals("Seattle", built.get("address").get("city").asText());
    }

    @Test
    void executeShouldReturnSpeedyResult() {
        CreateBuilder builder = new CreateBuilder("User", paths, sender, mapper, parser);
        SpeedyResult result = builder.field("name", "Alice").execute();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void buildShouldNotExecuteRequest() {
        AtomicReference<Boolean> sent = new AtomicReference<>(false);
        RequestSender countingSender = request -> {
            sent.set(true);
            return new SpeedyRawResponse(200, Collections.emptyMap(),
                    "{\"payload\":[{\"id\":1}],\"pageIndex\":0,\"pageSize\":10}");
        };
        CreateBuilder builder = new CreateBuilder("User", paths, countingSender, mapper, parser);
        builder.field("name", "Test").build();
        assertFalse(sent.get());
    }
}
