package com.github.silent.samurai.speedy.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.internal.PathBuilder;
import com.github.silent.samurai.speedy.client.internal.RequestSender;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GetBuilderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PathBuilder paths = new PathBuilder("http://localhost:8080", "/speedy/v1/");
    private final ResponseParser parser = new ResponseParser(mapper);

    @Test
    void getWithoutKeyShouldNotIncludeQueryString() {
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        RequestSender sender = request -> {
            capturedUrl.set(request.url());
            return new SpeedyRawResponse(200, Collections.emptyMap(),
                    "{\"payload\":[],\"pageIndex\":0,\"pageSize\":10}");
        };

        GetBuilder builder = new GetBuilder("User", paths, sender, mapper, parser);
        builder.execute();
        assertFalse(capturedUrl.get().contains("?"));
    }

    @Test
    void getWithKeyShouldIncludeQueryString() {
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        RequestSender sender = request -> {
            capturedUrl.set(request.url());
            return new SpeedyRawResponse(200, Collections.emptyMap(),
                    "{\"payload\":[],\"pageIndex\":0,\"pageSize\":10}");
        };

        GetBuilder builder = new GetBuilder("User", paths, sender, mapper, parser);
        builder.key("id", "123").execute();
        assertTrue(capturedUrl.get().contains("id=123"));
    }

    @Test
    void executeShouldReturnResult() {
        RequestSender sender = request -> new SpeedyRawResponse(200, Collections.emptyMap(),
                "{\"payload\":[{\"id\":1}],\"pageIndex\":0,\"pageSize\":10}");

        GetBuilder builder = new GetBuilder("User", paths, sender, mapper, parser);
        SpeedyResult result = builder.key("id", 1).execute();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
