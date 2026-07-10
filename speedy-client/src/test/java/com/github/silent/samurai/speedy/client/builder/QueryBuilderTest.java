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

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.junit.jupiter.api.Assertions.*;

class QueryBuilderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PathBuilder paths = new PathBuilder("http://localhost:8080", "/speedy/v1/");
    private final ResponseParser parser = new ResponseParser(mapper);

    @Test
    void whereShouldAddConditionToBody() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        RequestSender sender = request -> {
            capturedBody.set(request.body());
            return new SpeedyRawResponse(200, Collections.emptyMap(),
                    "{\"payload\":[],\"pageIndex\":0,\"pageSize\":10}");
        };

        QueryBuilder builder = new QueryBuilder("User", paths, sender, mapper, parser);
        builder.where(condition("active", eq(true))).execute();

        assertTrue(capturedBody.get().contains("$from"));
        assertTrue(capturedBody.get().contains("$where"));
        assertTrue(capturedBody.get().contains("active"));
    }

    @Test
    void orderByShouldSetSortDirection() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        RequestSender sender = request -> {
            capturedBody.set(request.body());
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        };

        QueryBuilder builder = new QueryBuilder("User", paths, sender, mapper, parser);
        builder.orderByAsc("name").orderByDesc("createdAt").execute();

        assertTrue(capturedBody.get().contains("\"ASC\""));
        assertTrue(capturedBody.get().contains("\"DESC\""));
    }

    @Test
    void paginationShouldSetIndexAndSize() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        RequestSender sender = request -> {
            capturedBody.set(request.body());
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        };

        QueryBuilder builder = new QueryBuilder("User", paths, sender, mapper, parser);
        builder.pageNo(2).pageSize(25).execute();

        assertTrue(capturedBody.get().contains("\"$index\""));
        assertTrue(capturedBody.get().contains("25"));
    }

    @Test
    void selectShouldAddFieldsToBody() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        RequestSender sender = request -> {
            capturedBody.set(request.body());
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        };

        QueryBuilder builder = new QueryBuilder("User", paths, sender, mapper, parser);
        builder.select("id", "name").execute();

        assertTrue(capturedBody.get().contains("\"$select\""));
        assertTrue(capturedBody.get().contains("\"id\""));
        assertTrue(capturedBody.get().contains("\"name\""));
    }

    @Test
    void expandShouldAddRelationsToBody() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        RequestSender sender = request -> {
            capturedBody.set(request.body());
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        };

        QueryBuilder builder = new QueryBuilder("User", paths, sender, mapper, parser);
        builder.expand("profile", "permissions").execute();

        assertTrue(capturedBody.get().contains("\"$expand\""));
        assertTrue(capturedBody.get().contains("\"profile\""));
    }

    @Test
    void countShouldReturnLong() {
        RequestSender sender = request -> new SpeedyRawResponse(200, Collections.emptyMap(),
                "{\"count\":15}");

        QueryBuilder builder = new QueryBuilder("User", paths, sender, mapper, parser);
        long count = builder.where(condition("active", eq(true))).count();
        assertEquals(15, count);
    }

    @Test
    void executeShouldReturnResult() {
        RequestSender sender = request -> new SpeedyRawResponse(200, Collections.emptyMap(),
                "{\"payload\":[{\"name\":\"Alice\"}],\"pageIndex\":0,\"pageSize\":10}");

        QueryBuilder builder = new QueryBuilder("User", paths, sender, mapper, parser);
        SpeedyResult result = builder.execute();
        assertNotNull(result);
    }

    @Test
    void buildShouldNotSendRequest() {
        AtomicReference<Boolean> sent = new AtomicReference<>(false);
        RequestSender sender = request -> {
            sent.set(true);
            return new SpeedyRawResponse(200, Collections.emptyMap(), "{\"payload\":[]}");
        };

        QueryBuilder builder = new QueryBuilder("User", paths, sender, mapper, parser);
        builder.where(condition("active", eq(true))).build();
        assertFalse(sent.get());
    }
}
