package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.silent.samurai.speedy.client.builder.CreateBuilder;
import com.github.silent.samurai.speedy.client.builder.DeleteBuilder;
import com.github.silent.samurai.speedy.client.builder.GetBuilder;
import com.github.silent.samurai.speedy.client.builder.QueryBuilder;
import com.github.silent.samurai.speedy.client.builder.UpdateBuilder;
import com.github.silent.samurai.speedy.client.exception.SpeedyConnectionException;
import com.github.silent.samurai.speedy.client.exception.SpeedyException;
import com.github.silent.samurai.speedy.client.internal.PathBuilder;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.JdkHttpTransport;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;
import com.github.silent.samurai.speedy.client.transport.SpeedyTransport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main entry point for the Speedy API production client.
 *
 * <p>Configured once via {@link #builder()} or {@link #connect(String)},
 * reused for all API calls. Returns fluent builders for CRUD and query operations.
 *
 * <pre>{@code
 * // Connect and create in under 10 lines
 * Speedy speedy = Speedy.connect("http://localhost:8080");
 * speedy.create("User").field("name", "Alice").field("email", "alice@example.com").execute();
 * User u = speedy.get("User").key("id", 1).execute().first(User.class);
 * speedy.update("User").key("id", 1).field("name", "Bob").execute();
 * speedy.delete("User").key("id", 1).execute();
 * }</pre>
 */
public class Speedy {

    private final SpeedyTransport transport;
    private final List<SpeedyInterceptor> interceptors;
    private final ObjectMapper mapper;
    private final PathBuilder paths;
    private final ResponseParser parser;

    private Speedy(String baseUrl, String apiPath, SpeedyTransport transport,
                   List<SpeedyInterceptor> interceptors, ObjectMapper mapper) {
        this.transport = transport;
        this.interceptors = interceptors != null ? interceptors : Collections.emptyList();
        this.mapper = mapper;
        this.paths = new PathBuilder(baseUrl, apiPath);
        this.parser = new ResponseParser(mapper);
    }

    /**
     * Quick-connect with defaults (JdkHttpTransport, default ObjectMapper).
     */
    public static Speedy connect(String baseUrl) {
        return builder().baseUrl(baseUrl).build();
    }

    /**
     * Returns a new {@link Builder} for configuring a {@code Speedy} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a create-builder for the given entity.
     */
    public CreateBuilder create(String entity) {
        return new CreateBuilder(entity, paths, this::send, mapper, parser);
    }

    /**
     * Creates a get-builder for the given entity.
     */
    public GetBuilder get(String entity) {
        return new GetBuilder(entity, paths, this::send, mapper, parser);
    }

    /**
     * Creates an update-builder for the given entity.
     */
    public UpdateBuilder update(String entity) {
        return new UpdateBuilder(entity, paths, this::send, mapper, parser);
    }

    /**
     * Creates a delete-builder for the given entity.
     */
    public DeleteBuilder delete(String entity) {
        return new DeleteBuilder(entity, paths, this::send, mapper, parser);
    }

    /**
     * Creates a query-builder for the given entity.
     */
    public QueryBuilder query(String entity) {
        return new QueryBuilder(entity, paths, this::send, mapper, parser);
    }

    /**
     * Bulk create multiple entities (provided as ObjectNode list).
     */
    public SpeedyResult createMany(String entity, List<ObjectNode> entities) {
        String url = paths.createPath(entity);
        String jsonBody;
        try {
            ArrayNode array = mapper.createArrayNode();
            for (ObjectNode entityNode : entities) {
                array.add(entityNode);
            }
            jsonBody = mapper.writeValueAsString(array);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
        SpeedyRequest request = new SpeedyRequest("POST", url, Collections.emptyMap(), jsonBody);
        try {
            SpeedyRawResponse response = send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("CreateMany request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Bulk delete entities by primary key array.
     */
    public SpeedyResult deleteMany(String entity, List<ObjectNode> pks) {
        String url = paths.deletePath(entity);
        String jsonBody;
        try {
            ArrayNode array = mapper.createArrayNode();
            for (ObjectNode pk : pks) {
                array.add(pk);
            }
            jsonBody = mapper.writeValueAsString(array);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
        SpeedyRequest request = new SpeedyRequest("DELETE", url, Collections.emptyMap(), jsonBody);
        try {
            SpeedyRawResponse response = send(request);
            return parser.parseEntityResponse(response);
        } catch (IOException e) {
            throw new SpeedyConnectionException("DeleteMany request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches the API metadata.
     */
    public JsonNode metadata() {
        String url = paths.metadataPath();
        SpeedyRequest request = new SpeedyRequest("GET", url, Collections.emptyMap(), null);
        try {
            SpeedyRawResponse response = send(request);
            if (!response.is2xx()) {
                throw parser.parseError(response);
            }
            String body = response.body();
            if (body == null || body.isEmpty()) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(body);
        } catch (IOException e) {
            throw new SpeedyConnectionException("Metadata request failed: " + e.getMessage(), e);
        }
    }

    SpeedyRawResponse send(SpeedyRequest request) throws IOException {
        SpeedyRequest current = request;
        for (SpeedyInterceptor interceptor : interceptors) {
            current = interceptor.intercept(current);
        }
        return transport.send(current);
    }

    /**
     * Fluent builder for constructing {@link Speedy} instances.
     */
    public static class Builder {
        private String baseUrl;
        private String apiPath = "/speedy/v1/";
        private SpeedyTransport transport;
        private final List<SpeedyInterceptor> interceptors = new ArrayList<>();
        private ObjectMapper objectMapper;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiPath(String apiPath) {
            this.apiPath = apiPath;
            return this;
        }

        public Builder transport(SpeedyTransport transport) {
            this.transport = transport;
            return this;
        }

        public Builder interceptor(SpeedyInterceptor interceptor) {
            this.interceptors.add(interceptor);
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Speedy build() {
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalStateException("baseUrl is required");
            }
            if (transport == null) {
                transport = new JdkHttpTransport();
            }
            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                objectMapper.configure(
                        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            }
            return new Speedy(baseUrl, apiPath, transport, interceptors, objectMapper);
        }
    }
}
