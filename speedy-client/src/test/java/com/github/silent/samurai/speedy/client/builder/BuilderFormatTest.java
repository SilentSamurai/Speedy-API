package com.github.silent.samurai.speedy.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.client.format.JsonFormat;
import com.github.silent.samurai.speedy.client.format.SpeedyFormat;
import com.github.silent.samurai.speedy.client.format.XmlFormat;
import com.github.silent.samurai.speedy.client.format.YamlFormat;
import com.github.silent.samurai.speedy.client.internal.PathBuilder;
import com.github.silent.samurai.speedy.client.internal.RequestSender;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Builders must serialize the request body with the configured wire format and
 * carry the matching Content-Type/Accept headers for server-side negotiation.
 */
class BuilderFormatTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PathBuilder paths = new PathBuilder("http://localhost:8080", "/speedy/v1/");

    private final AtomicReference<SpeedyRequest> captured = new AtomicReference<>();
    private final RequestSender sender = request -> {
        captured.set(request);
        return new SpeedyRawResponse(200, Map.of(), null);
    };

    @Test
    void createWithXmlFormatShouldSendXmlBodyAndHeaders() {
        XmlFormat format = new XmlFormat();
        new CreateBuilder("Product", paths, sender, mapper, new ResponseParser(mapper, format), format)
                .field("name", "Widget")
                .execute();

        SpeedyRequest request = captured.get();
        assertEquals(List.of(format.contentType()), request.headers().get("Content-Type"));
        assertEquals(List.of(format.contentType()), request.headers().get("Accept"));
        assertTrue(request.body().contains("<root><entity><name>Widget</name></entity></root>"), request.body());
    }

    @Test
    void queryWithYamlFormatShouldSendYamlBodyAndHeaders() {
        YamlFormat format = new YamlFormat();
        new QueryBuilder("Product", paths, sender, mapper, new ResponseParser(mapper, format), format)
                .pageSize(5)
                .execute();

        SpeedyRequest request = captured.get();
        assertEquals(List.of(format.contentType()), request.headers().get("Content-Type"));
        assertEquals(List.of(format.contentType()), request.headers().get("Accept"));
        assertTrue(request.body().contains("$from: \"Product\""), request.body());
    }

    @Test
    void getShouldSendAcceptButNoContentType() {
        SpeedyFormat format = new XmlFormat();
        new GetBuilder("Product", paths, sender, mapper, new ResponseParser(mapper, format), format)
                .key("id", 1)
                .execute();

        SpeedyRequest request = captured.get();
        assertEquals(List.of(format.contentType()), request.headers().get("Accept"));
        assertFalse(request.headers().containsKey("Content-Type"));
        assertNull(request.body());
    }

    @Test
    void defaultConstructorShouldSendJsonHeaders() {
        new CreateBuilder("Product", paths, sender, mapper, new ResponseParser(mapper))
                .field("name", "Widget")
                .execute();

        SpeedyRequest request = captured.get();
        assertEquals(List.of(JsonFormat.CONTENT_TYPE), request.headers().get("Content-Type"));
        assertTrue(request.body().startsWith("[{"), request.body());
    }
}
