package com.github.silent.samurai.speedy.client.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.exception.SpeedyBadRequestException;
import com.github.silent.samurai.speedy.client.exception.SpeedyDeserializationException;
import com.github.silent.samurai.speedy.client.exception.SpeedyException;
import com.github.silent.samurai.speedy.client.exception.SpeedyNotFoundException;
import com.github.silent.samurai.speedy.client.exception.SpeedyServerException;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ResponseParserTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private final ResponseParser parser = new ResponseParser(mapper);

    @Test
    void parseEntityResponseShouldReturnResultFor2xx() {
        String body = "{\"payload\":[{\"id\":1,\"name\":\"Alice\"}],\"pageIndex\":0,\"pageSize\":10}";
        SpeedyRawResponse response = new SpeedyRawResponse(200, Map.of(), body);

        SpeedyResult result = parser.parseEntityResponse(response);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(0, result.pageIndex());
        assertEquals(10, result.pageSize());
    }

    @Test
    void parseEntityResponseShouldHandleEmptyArray() {
        String body = "{\"payload\":[],\"pageIndex\":0,\"pageSize\":10}";
        SpeedyRawResponse response = new SpeedyRawResponse(200, Map.of(), body);

        SpeedyResult result = parser.parseEntityResponse(response);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseEntityResponseShouldHandleNullBody() {
        SpeedyRawResponse response = new SpeedyRawResponse(200, Map.of(), null);

        SpeedyResult result = parser.parseEntityResponse(response);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseEntityResponseShouldHandleEmptyBody() {
        SpeedyRawResponse response = new SpeedyRawResponse(200, Map.of(), "");

        SpeedyResult result = parser.parseEntityResponse(response);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseEntityResponseShouldHandleMissingPayload() {
        String body = "{\"pageIndex\":0,\"pageSize\":10}";
        SpeedyRawResponse response = new SpeedyRawResponse(200, Map.of(), body);

        SpeedyResult result = parser.parseEntityResponse(response);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseEntityResponseShouldThrowForNonJson() {
        SpeedyRawResponse response = new SpeedyRawResponse(200, Map.of(), "<html>error</html>");
        assertThrows(SpeedyDeserializationException.class, () -> parser.parseEntityResponse(response));
    }

    @Test
    void parseEntityResponseShouldThrowBadRequestFor400() {
        String body = "{\"status\":400,\"message\":\"Validation failed\",\"timestamp\":\"2024-01-01T00:00:00Z\"}";
        SpeedyRawResponse response = new SpeedyRawResponse(400, Map.of(), body);
        assertThrows(SpeedyBadRequestException.class, () -> parser.parseEntityResponse(response));
    }

    @Test
    void parseEntityResponseShouldThrowNotFoundFor404() {
        SpeedyRawResponse response = new SpeedyRawResponse(404, Map.of(), null);
        assertThrows(SpeedyNotFoundException.class, () -> parser.parseEntityResponse(response));
    }

    @Test
    void parseEntityResponseShouldThrowServerExceptionFor500() {
        SpeedyRawResponse response = new SpeedyRawResponse(500, Map.of(), null);
        assertThrows(SpeedyServerException.class, () -> parser.parseEntityResponse(response));
    }

    @Test
    void parseCountResponseShouldReturnCount() {
        String body = "{\"count\":42}";
        SpeedyRawResponse response = new SpeedyRawResponse(200, Map.of(), body);

        long count = parser.parseCountResponse(response);
        assertEquals(42, count);
    }

    @Test
    void parseCountResponseShouldReturnZeroForMissingCount() {
        String body = "{}";
        SpeedyRawResponse response = new SpeedyRawResponse(200, Map.of(), body);

        long count = parser.parseCountResponse(response);
        assertEquals(0, count);
    }

    @Test
    void parseCountResponseShouldThrowFor4xx() {
        SpeedyRawResponse response = new SpeedyRawResponse(400, Map.of(), null);
        assertThrows(SpeedyException.class, () -> parser.parseCountResponse(response));
    }

    @Test
    void parseErrorShouldIncludeServerMessage() {
        String body = "{\"status\":400,\"message\":\"Bad input\",\"timestamp\":\"2024-01-01T00:00:00Z\"}";
        SpeedyRawResponse response = new SpeedyRawResponse(400, Map.of(), body);

        SpeedyException ex = parser.parseError(response);
        assertInstanceOf(SpeedyBadRequestException.class, ex);
        assertEquals(400, ex.statusCode());
        assertEquals("Bad input", ex.serverMessage());
        assertEquals("2024-01-01T00:00:00Z", ex.timestamp());
    }

    @Test
    void parseErrorShouldHandleNonJsonBody() {
        String body = "Connection refused";
        SpeedyRawResponse response = new SpeedyRawResponse(502, Map.of(), body);

        SpeedyException ex = parser.parseError(response);
        assertInstanceOf(SpeedyServerException.class, ex);
        assertEquals(502, ex.statusCode());
        assertEquals("Connection refused", ex.serverMessage());
    }

    @Test
    void parseErrorShouldReturnGenericFor4xxOtherThan400or404() {
        SpeedyRawResponse response = new SpeedyRawResponse(403, Map.of(), "{}");
        SpeedyException ex = parser.parseError(response);
        assertEquals(403, ex.statusCode());
        assertFalse(ex instanceof SpeedyBadRequestException);
        assertFalse(ex instanceof SpeedyNotFoundException);
    }
}
