# Speedy Java Client - Redesign Plan

## Executive Summary

Full redesign of `speedy-java-client` to fix fundamental issues:

- **Broken client-server contract** (`asCount()` doesn't work, `totalPageCount` is never sent)
- **Generic `<T>` complexity** leaks transport concerns into user-facing API
- **No error handling** — raw `HttpClientErrorException` leaks to users
- **No auth/header support** — requires custom `HttpClient<T>` implementation
- **Three conflicting ObjectMapper instances**
- **Framework coupling** — core depends on Spring types (`HttpHeaders`, `MultiValueMap`, `HttpMethod`)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                          User Code                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  PRODUCTION                          TESTING                         │
│  ──────────                          ───────                         │
│  Speedy speedy = Speedy.builder()    SpeedyTest test =               │
│      .baseUrl("http://host:8080")        SpeedyTest.mockMvc(mvc);    │
│      .interceptor(req -> ...)                                        │
│      .build();                       test.create("User")             │
│                                          .field("name", "")          │
│  speedy.create("User")                   .execute()                  │
│      .field("name", "Alice")             .expectStatus(400);         │
│      .execute()                                                      │
│      .first(User.class);                                             │
│                                                                      │
└────────────────────────────┬─────────────────────┬──────────────────┘
                             │                     │
                             ▼                     ▼
┌────────────────────────────────────┐  ┌────────────────────────────┐
│       Speedy (Production)          │  │    SpeedyTest (Testing)    │
│  - Returns SpeedyResult            │  │  - Returns SpeedyTestResult│
│  - Throws SpeedyException          │  │  - Assertion methods       │
│  - Interceptor chain               │  │  - ResultActions escape    │
├────────────────────────────────────┤  ├────────────────────────────┤
│       Shared: Builders, Query DSL, PathBuilder, FieldBuilder        │
├─────────────────────────────────────────────────────────────────────┤
│                    ResponseParser                                     │
│  - 2xx + {"payload":[...]} → SpeedyResult                           │
│  - 2xx + {"count": N}      → long                                   │
│  - 4xx/5xx + {"status":..., "message":...} → SpeedyException        │
├─────────────────────────────────────────────────────────────────────┤
│                SpeedyTransport (SPI)                                  │
│  SpeedyRequest → SpeedyRawResponse                                   │
├──────────────┬───────────────────┬──────────────────────────────────┤
│ JdkTransport │ RestTemplateTrans │ MockMvcTransport                  │
│ (default)    │ (optional/spring) │ (optional/spring-test)            │
└──────────────┴───────────────────┴──────────────────────────────────┘
```

---

## Module Structure

Single module with optional dependencies:

```
speedy-java-client/
├── pom.xml
├── src/main/java/com/github/silent/samurai/speedy/client/
│   │
│   ├── Speedy.java                          # Main entry point + builder
│   ├── SpeedyResult.java                    # Typed response wrapper
│   ├── SpeedyQuery.java                     # Query DSL (static operators)
│   ├── SpeedyInterceptor.java              # Request interceptor FI
│   │
│   ├── exception/
│   │   ├── SpeedyException.java             # Base unchecked exception
│   │   ├── SpeedyBadRequestException.java   # 400
│   │   ├── SpeedyNotFoundException.java     # 404
│   │   ├── SpeedyServerException.java       # 5xx
│   │   └── SpeedyConnectionException.java   # Network/timeout failures
│   │
│   ├── builder/
│   │   ├── CreateBuilder.java               # Fluent create builder
│   │   ├── GetBuilder.java                  # Fluent get builder
│   │   ├── UpdateBuilder.java               # Fluent update builder
│   │   ├── DeleteBuilder.java               # Fluent delete builder
│   │   └── QueryBuilder.java               # Fluent query builder (integrates SpeedyQuery)
│   │
│   ├── transport/
│   │   ├── SpeedyTransport.java             # SPI interface
│   │   ├── SpeedyRequest.java               # Immutable request VO
│   │   ├── SpeedyRawResponse.java           # Immutable response VO
│   │   └── JdkHttpTransport.java            # Default impl (java.net.http)
│   │
│   ├── spring/                              # Optional (requires spring-web)
│   │   └── RestTemplateTransport.java       # Spring RestTemplate adapter
│   │
│   ├── test/                                # Optional (requires spring-test)
│   │   ├── SpeedyTest.java                  # MockMvc test facade
│   │   ├── SpeedyTestResult.java            # Assertion-oriented result
│   │   └── MockMvcTransport.java            # MockMvc adapter
│   │
│   └── internal/
│       ├── ResponseParser.java              # Raw body → SpeedyResult or exception
│       ├── PathBuilder.java                 # URL path construction
│       └── FieldUtil.java                   # Shared dot-notation field logic
│
└── src/test/java/...
```

---

## Dependency Strategy

```xml
<dependencies>
    <!-- Required: JSON processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>

    <!-- Optional: Spring RestTemplate transport -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Optional: MockMvc test transport -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-test</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Optional: Servlet API for MockMvc -->
    <dependency>
        <groupId>jakarta.servlet</groupId>
        <artifactId>jakarta.servlet-api</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**Key change:** `speedy-commons` dependency is **removed**. The client should not depend on server internals. Only
Jackson is needed.

---

## Public API Design

### 1. `Speedy` — Production Entry Point

```java
package com.github.silent.samurai.speedy.client;

public class Speedy {

    // ─── Construction ───────────────────────────────────────────────

    /**
     * Minimal construction with JDK HttpClient.
     */
    public static Speedy connect(String baseUrl) { ...}

    /**
     * Full builder for advanced configuration.
     */
    public static Builder builder() { ...}

    public static class Builder {
        public Builder baseUrl(String url) { ...}

        public Builder transport(SpeedyTransport transport) { ...}

        public Builder interceptor(SpeedyInterceptor interceptor) { ...}

        public Builder objectMapper(ObjectMapper mapper) { ...}

        public Builder apiPath(String path) { ...}  // default: "/speedy/v1/"

        public Speedy build() { ...}
    }

    // ─── CRUD Operations ────────────────────────────────────────────

    public CreateBuilder create(String entity) { ...}

    public GetBuilder get(String entity) { ...}

    public UpdateBuilder update(String entity) { ...}

    public DeleteBuilder delete(String entity) { ...}

    // ─── Query ──────────────────────────────────────────────────────

    public QueryBuilder query(String entity) { ...}

    // ─── Bulk Operations ────────────────────────────────────────────

    public SpeedyResult createMany(String entity, List<ObjectNode> entities) { ...}

    public SpeedyResult deleteMany(String entity, List<ObjectNode> pks) { ...}

    // ─── Metadata ───────────────────────────────────────────────────

    public JsonNode metadata() { ...}
}
```

### 2. `SpeedyResult` — Typed Response Wrapper

```java
package com.github.silent.samurai.speedy.client;

public class SpeedyResult {

    // ─── Typed Deserialization ───────────────────────────────────────

    /** Deserialize payload as a list of the given type. */
    public <T> List<T> list(Class<T> type) { ... }

    /** Deserialize first entity from payload. */
    public <T> T first(Class<T> type) { ... }

    /** Deserialize first entity, returning Optional.empty() if not found. */
    public <T> Optional<T> firstOptional(Class<T> type) { ... }

    // ─── Raw Access ─────────────────────────────────────────────────

    /** Raw payload as JsonNode (the array). */
    public JsonNode raw() { ... }

    /** First element as raw JsonNode. */
    public JsonNode firstRaw() { ... }

    // ─── Pagination ─────────────────────────────────────────────────

    public int pageIndex() { ... }
    public int pageSize() { ... }
    public boolean isEmpty() { ... }
    public int size() { ... }
}
```

### 3. `SpeedyQuery` — Static Query Operators (unchanged DSL)

```java
package com.github.silent.samurai.speedy.client;

/**
 * Static factory methods for query condition operators.
 * Use with static imports for a clean DSL.
 *
 * import static com.github.silent.samurai.speedy.client.SpeedyQuery.*;
 */
public final class SpeedyQuery {

    // ─── Comparison Operators ───────────────────────────────────────

    public static ObjectNode eq(Object value) { ... }
    public static ObjectNode ne(Object value) { ... }
    public static ObjectNode gt(Object value) { ... }
    public static ObjectNode lt(Object value) { ... }
    public static ObjectNode gte(Object value) { ... }
    public static ObjectNode lte(Object value) { ... }
    public static ObjectNode in(Object... values) { ... }
    public static ObjectNode nin(Object... values) { ... }
    public static ObjectNode matches(Object value) { ... }
    public static ObjectNode contains(Object value) { ... }

    // ─── Logical Operators ──────────────────────────────────────────

    public static ObjectNode and(JsonNode... conditions) { ... }
    public static ObjectNode or(JsonNode... conditions) { ... }

    // ─── Condition Builder ──────────────────────────────────────────

    public static JsonNode condition(String field, JsonNode operator) { ... }
}
```

### 4. `QueryBuilder` — Fluent Query (Integrated)

```java
package com.github.silent.samurai.speedy.client.builder;

public class QueryBuilder {

    // ─── Filtering ──────────────────────────────────────────────────

    /** Set WHERE conditions. Accepts condition(), and(), or() nodes. */
    public QueryBuilder where(JsonNode... conditions) { ... }

    // ─── Ordering ───────────────────────────────────────────────────

    public QueryBuilder orderByAsc(String field) { ... }
    public QueryBuilder orderByDesc(String field) { ... }

    // ─── Pagination ─────────────────────────────────────────────────

    public QueryBuilder pageNo(int page) { ... }
    public QueryBuilder pageSize(int size) { ... }

    // ─── Projection ─────────────────────────────────────────────────

    public QueryBuilder select(String... fields) { ... }
    public QueryBuilder expand(String... relations) { ... }

    // ─── Terminal Operations ────────────────────────────────────────

    /** Execute query and return results. */
    public SpeedyResult execute() { ... }

    /** Execute count query and return the count directly. */
    public long count() { ... }

    // ─── Build Without Executing ────────────────────────────────────

    /** Build the query JSON without executing (for inspection/reuse). */
    public JsonNode build() { ... }
}
```

### 5. CRUD Builders

```java
// ─── CreateBuilder ──────────────────────────────────────────────────

public class CreateBuilder {
    public CreateBuilder field(String name, Object value) { ... }  // supports dot-notation
    public SpeedyResult execute() { ... }
    public ObjectNode build() { ... }  // build without executing
}

// ─── GetBuilder ─────────────────────────────────────────────────────

public class GetBuilder {
    public GetBuilder key(String field, Object value) { ... }
    public SpeedyResult execute() { ... }
}

// ─── UpdateBuilder ──────────────────────────────────────────────────

public class UpdateBuilder {
    public UpdateBuilder key(String field, Object value) { ... }
    public UpdateBuilder field(String name, Object value) { ... }  // supports dot-notation
    public SpeedyResult execute() { ... }
    public ObjectNode build() { ... }
}

// ─── DeleteBuilder ──────────────────────────────────────────────────

public class DeleteBuilder {
    public DeleteBuilder key(String field, Object value) { ... }
    public SpeedyResult execute() { ... }
}
```

### 6. Transport SPI

```java
package com.github.silent.samurai.speedy.client.transport;

// ─── Interface ──────────────────────────────────────────────────────

@FunctionalInterface
public interface SpeedyTransport {
    SpeedyRawResponse send(SpeedyRequest request) throws IOException;
}

// ─── Request (Immutable) ────────────────────────────────────────────

public final class SpeedyRequest {
    private final String method;                         // GET, POST, PATCH, DELETE
    private final String url;                            // Full URL
    private final Map<String, List<String>> headers;     // Immutable
    private final String body;                           // JSON string or null

    // Builder for interceptors to create modified copies
    public SpeedyRequest withHeader(String name, String value) { ... }
    public SpeedyRequest withHeaders(Map<String, String> headers) { ... }
}

// ─── Response (Immutable) ───────────────────────────────────────────

public final class SpeedyRawResponse {
    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final String body;
}
```

### 7. Interceptor

```java
package com.github.silent.samurai.speedy.client;

@FunctionalInterface
public interface SpeedyInterceptor {
    SpeedyRequest intercept(SpeedyRequest request);
}
```

### 8. Exception Hierarchy

```java
package com.github.silent.samurai.speedy.client.exception;

/**
 * Base exception for all Speedy client errors. Unchecked.
 */
public class SpeedyException extends RuntimeException {
    private final int statusCode;
    private final String serverMessage;
    private final String timestamp;
    private final String responseBody;    // raw body for debugging

    public int statusCode() { ... }
    public String serverMessage() { ... }
    public String timestamp() { ... }
    public String responseBody() { ... }
}

/** Server returned 400 Bad Request. */
public class SpeedyBadRequestException extends SpeedyException { }

/** Server returned 404 Not Found. */
public class SpeedyNotFoundException extends SpeedyException { }

/** Server returned 5xx. */
public class SpeedyServerException extends SpeedyException { }

/** Network error, timeout, connection refused. */
public class SpeedyConnectionException extends SpeedyException { }

/** JSON deserialization failure. */
public class SpeedyDeserializationException extends SpeedyException { }
```

### 9. `SpeedyTest` — MockMvc Test Facade

```java
package com.github.silent.samurai.speedy.client.test;

public class SpeedyTest {

    // ─── Construction ───────────────────────────────────────────────

    public static SpeedyTest mockMvc(MockMvc mockMvc) { ... }

    // ─── Same builder API as Speedy ─────────────────────────────────

    public TestCreateBuilder create(String entity) { ... }
    public TestGetBuilder get(String entity) { ... }
    public TestUpdateBuilder update(String entity) { ... }
    public TestDeleteBuilder delete(String entity) { ... }
    public TestQueryBuilder query(String entity) { ... }
}
```

### 10. `SpeedyTestResult` — Assertion-Oriented Result

```java
package com.github.silent.samurai.speedy.client.test;

public class SpeedyTestResult {

    // ─── Status Assertions ──────────────────────────────────────────

    public SpeedyTestResult expectStatus(int expectedStatus) { ... }
    public SpeedyTestResult expectOk() { ... }          // 200
    public SpeedyTestResult expectCreated() { ... }     // 201
    public SpeedyTestResult expectBadRequest() { ... }  // 400
    public SpeedyTestResult expectNotFound() { ... }    // 404

    // ─── JSON Assertions ────────────────────────────────────────────

    /** Assert JSONPath value with Hamcrest matcher. */
    public SpeedyTestResult expectJsonPath(String path, Matcher<?> matcher) { ... }

    /** Assert JSONPath value equals expected. */
    public SpeedyTestResult expectJsonPath(String path, Object expected) { ... }

    /** Assert JSONPath exists. */
    public SpeedyTestResult expectJsonPathExists(String path) { ... }

    // ─── Data Extraction ────────────────────────────────────────────

    /** Extract a value via JSONPath for use in subsequent test steps. */
    public <T> T jsonPath(String path, Class<T> type) { ... }
    public String jsonPath(String path) { ... }

    // ─── Typed Access (same as SpeedyResult) ────────────────────────

    public <T> List<T> list(Class<T> type) { ... }
    public <T> T first(Class<T> type) { ... }

    // ─── Escape Hatch ───────────────────────────────────────────────

    /** Access raw MockMvc ResultActions for advanced assertions. */
    public ResultActions resultActions() { ... }

    /** Access raw response body as string. */
    public String responseBody() { ... }
}
```

---

## Usage Examples

### Production CRUD

```java
import static com.github.silent.samurai.speedy.client.SpeedyQuery.*;

// ─── Setup (once) ───────────────────────────────────────────────────

Speedy speedy = Speedy.builder()
        .baseUrl("http://localhost:8080")
        .interceptor(req -> req.withHeader("Authorization", "Bearer " + jwt))
        .interceptor(req -> req.withHeader("X-Tenant-Id", tenantId))
        .build();

// ─── Create ─────────────────────────────────────────────────────────

User created = speedy.create("User")
        .field("name", "Alice")
        .field("email", "alice@example.com")
        .field("address.city", "Seattle")       // dot-notation for nested/FK
        .execute()
        .first(User.class);

System.out.

println("Created user: "+created.getId());

// ─── Get by PK ──────────────────────────────────────────────────────

User user = speedy.get("User")
        .key("id", created.getId())
        .execute()
        .first(User.class);

// ─── Update ─────────────────────────────────────────────────────────

speedy.

update("User")
    .

key("id",user.getId())
        .

field("name","Bob")
    .

field("address.city","Portland")
    .

execute();

// ─── Delete ─────────────────────────────────────────────────────────

speedy.

delete("User")
    .

key("id",user.getId())
        .

execute();

// ─── Query ──────────────────────────────────────────────────────────

List<User> activeAdults = speedy.query("User")
        .where(
                and(
                        condition("active", eq(true)),
                        condition("age", gte(18))
                )
        )
        .orderByAsc("name")
        .pageSize(20)
        .pageNo(0)
        .expand("profile", "permissions")
        .select("id", "name", "email", "age")
        .execute()
        .list(User.class);

// ─── Count ──────────────────────────────────────────────────────────

long totalActive = speedy.query("User")
        .where(condition("active", eq(true)))
        .count();

// ─── Metadata ───────────────────────────────────────────────────────

JsonNode metadata = speedy.metadata();
```

### Testing with MockMvc

```java
import static com.github.silent.samurai.speedy.client.SpeedyQuery.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = MOCK)
@AutoConfigureMockMvc(addFilters = false)
class UserIntegrationTest {

    @Autowired MockMvc mockMvc;
    SpeedyTest speedy;

    @BeforeEach
    void setUp() {
        speedy = SpeedyTest.mockMvc(mockMvc);
    }

    @Test
    void createWithInvalidData_shouldReturn400() {
        speedy.create("User")
            .field("name", "")
            .execute()
            .expectBadRequest()
            .expectJsonPath("$.message", containsString("name"));
    }

    @Test
    void fullCrudWorkflow() {
        // Create
        String id = speedy.create("User")
            .field("name", "Alice")
            .field("email", "alice@example.com")
            .execute()
            .expectOk()
            .expectJsonPathExists("$.payload[0].id")
            .jsonPath("$.payload[0].id");

        // Read
        speedy.get("User")
            .key("id", id)
            .execute()
            .expectOk()
            .expectJsonPath("$.payload[0].name", "Alice");

        // Update
        speedy.update("User")
            .key("id", id)
            .field("name", "Bob")
            .execute()
            .expectOk();

        // Verify update
        speedy.get("User")
            .key("id", id)
            .execute()
            .expectOk()
            .expectJsonPath("$.payload[0].name", "Bob");

        // Delete
        speedy.delete("User")
            .key("id", id)
            .execute()
            .expectOk();
    }

    @Test
    void queryWithPagination() {
        speedy.query("Product")
            .where(condition("price", gte(100)))
            .orderByDesc("price")
            .pageSize(5)
            .execute()
            .expectOk()
            .expectJsonPath("$.payload.length()", lessThanOrEqualTo(5));
    }

    @Test
    void advancedMockMvcAssertions() {
        // Escape hatch to raw ResultActions when needed
        ResultActions actions = speedy.create("User")
            .field("name", "Test")
            .execute()
            .resultActions();

        actions.andExpect(status().isOk())
               .andExpect(jsonPath("$.payload[0].id").isString());
    }
}
```

### Custom Transport Implementation

```java
// OkHttp example
public class OkHttpTransport implements SpeedyTransport {

    private final OkHttpClient client;

    public OkHttpTransport(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public SpeedyRawResponse send(SpeedyRequest request) throws IOException {
        Request.Builder builder = new Request.Builder()
            .url(request.url());

        // Add headers
        request.headers().forEach((name, values) ->
            values.forEach(v -> builder.addHeader(name, v)));

        // Build request body
        RequestBody body = request.body() != null
            ? RequestBody.create(request.body(), MediaType.parse("application/json"))
            : null;

        builder.method(request.method(), body);

        try (Response response = client.newCall(builder.build()).execute()) {
            return new SpeedyRawResponse(
                response.code(),
                response.headers().toMultimap(),
                response.body() != null ? response.body().string() : null
            );
        }
    }
}

// Usage
Speedy speedy = Speedy.builder()
    .baseUrl("http://localhost:8080")
    .transport(new OkHttpTransport(okHttpClient))
    .build();
```

### Multi-Tenancy with Interceptors

```java
Speedy speedy = Speedy.builder()
        .baseUrl("http://localhost:8080")
        .interceptor(req -> req.withHeader("Authorization", "Bearer " + tokenProvider.getToken()))
        .interceptor(req -> req.withHeader("X-Tenant-Id", TenantContext.current()))
        .interceptor(req -> req.withHeader("X-Correlation-Id", UUID.randomUUID().toString()))
        .build();
```

---

## Internal Implementation Details

### ResponseParser — Central Response Handling

```java
package com.github.silent.samurai.speedy.client.internal;

/**
 * Parses raw HTTP responses into typed results or exceptions.
 * This is the SINGLE place where the client-server contract is enforced.
 */
class ResponseParser {

    private final ObjectMapper mapper;

    ResponseParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Parse a successful entity response.
     * Expected format: {"payload": [...], "pageIndex": N, "pageSize": N}
     */
    SpeedyResult parseEntityResponse(SpeedyRawResponse response) {
        if (response.statusCode() >= 400) {
            throw parseError(response);
        }
        JsonNode root = mapper.readTree(response.body());
        JsonNode payload = root.get("payload");
        int pageIndex = root.has("pageIndex") ? root.get("pageIndex").asInt(0) : 0;
        int pageSize = root.has("pageSize") ? root.get("pageSize").asInt(0) : 0;
        return new SpeedyResult(payload, pageIndex, pageSize, mapper);
    }

    /**
     * Parse a count response.
     * Expected format: {"count": N}
     */
    long parseCountResponse(SpeedyRawResponse response) {
        if (response.statusCode() >= 400) {
            throw parseError(response);
        }
        JsonNode root = mapper.readTree(response.body());
        return root.get("count").asLong(0L);
    }

    /**
     * Parse an error response into a typed exception.
     * Expected format: {"status": N, "message": "...", "timestamp": "..."}
     */
    SpeedyException parseError(SpeedyRawResponse response) {
        int status = response.statusCode();
        String serverMessage = null;
        String timestamp = null;

        try {
            JsonNode root = mapper.readTree(response.body());
            serverMessage = root.has("message") ? root.get("message").asText() : null;
            timestamp = root.has("timestamp") ? root.get("timestamp").asText() : null;
        } catch (Exception ignored) {
            // Response body isn't valid JSON — use raw body as message
            serverMessage = response.body();
        }

        return switch (status / 100) {
            case 4 -> switch (status) {
                case 400 -> new SpeedyBadRequestException(status, serverMessage, timestamp, response.body());
                case 404 -> new SpeedyNotFoundException(status, serverMessage, timestamp, response.body());
                default -> new SpeedyException(status, serverMessage, timestamp, response.body());
            };
            case 5 -> new SpeedyServerException(status, serverMessage, timestamp, response.body());
            default -> new SpeedyException(status, serverMessage, timestamp, response.body());
        };
    }
}
```

### PathBuilder — URL Construction

```java
package com.github.silent.samurai.speedy.client.internal;

/**
 * Builds API endpoint URLs.
 */
class PathBuilder {

    private final String baseUrl;   // e.g., "http://localhost:8080"
    private final String apiPath;   // e.g., "/speedy/v1/"

    String entityPath(String entity) {
        return baseUrl + apiPath + entity;
    }

    String createPath(String entity) {
        return entityPath(entity) + "/$create";
    }

    String updatePath(String entity) {
        return entityPath(entity) + "/$update";
    }

    String deletePath(String entity) {
        return entityPath(entity) + "/$delete";
    }

    String queryPath(String entity) {
        return entityPath(entity) + "/$query";
    }

    String countPath(String entity) {
        return entityPath(entity) + "/$count";
    }

    String metadataPath() {
        return baseUrl + apiPath + "$metadata";
    }

    /**
     * Format PK as query string: ?id=123&type=A
     */
    String formatPk(ObjectNode pk) {
        if (pk == null || pk.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("?");
        Iterator<Map.Entry<String, JsonNode>> fields = pk.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (sb.length() > 1) sb.append("&");
            sb.append(URLEncoder.encode(entry.getKey(), UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue().asText(), UTF_8));
        }
        return sb.toString();
    }
}
```

### FieldUtil — Shared Dot-Notation Logic

```java
package com.github.silent.samurai.speedy.client.internal;

/**
 * Shared utility for setting nested fields using dot-notation paths.
 * e.g., "address.city" → {"address": {"city": value}}
 */
class FieldUtil {

    private final ObjectMapper mapper;

    /**
     * Set a field on the given ObjectNode using dot-notation path.
     */
    void setField(ObjectNode root, String path, Object value) {
        String[] parts = path.split("\\.");
        ObjectNode current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.has(part)) {
                current.set(part, mapper.createObjectNode());
            }
            current = (ObjectNode) current.get(part);
        }

        current.set(parts[parts.length - 1], mapper.convertValue(value, JsonNode.class));
    }
}
```

### JdkHttpTransport — Default Transport (No Spring)

```java
package com.github.silent.samurai.speedy.client.transport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Default transport using JDK 11+ java.net.http.HttpClient.
 * Zero external dependencies.
 */
public class JdkHttpTransport implements SpeedyTransport {

    private final HttpClient httpClient;

    public JdkHttpTransport() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public JdkHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public SpeedyRawResponse send(SpeedyRequest request) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(request.url()))
                .timeout(Duration.ofSeconds(60));

        // Set headers
        request.headers().forEach((name, values) ->
                values.forEach(v -> builder.header(name, v)));
        builder.header("Content-Type", "application/json");
        builder.header("Accept", "application/json");

        // Set method + body
        HttpRequest.BodyPublisher bodyPublisher = request.body() != null
                ? HttpRequest.BodyPublishers.ofString(request.body())
                : HttpRequest.BodyPublishers.noBody();

        builder.method(request.method(), bodyPublisher);

        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            Map<String, List<String>> responseHeaders = response.headers().map();
            return new SpeedyRawResponse(response.statusCode(), responseHeaders, response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }
}
```

### RestTemplateTransport — Spring Adapter

```java
package com.github.silent.samurai.speedy.client.spring;

import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * SpeedyTransport implementation using Spring's RestTemplate.
 * Optional dependency — only available when spring-web is on classpath.
 */
public class RestTemplateTransport implements SpeedyTransport {

    private final RestTemplate restTemplate;

    public RestTemplateTransport(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public SpeedyRawResponse send(SpeedyRequest request) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        request.headers().forEach((name, values) -> headers.put(name, values));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = request.body() != null
                ? new HttpEntity<>(request.body(), headers)
                : new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    request.url(),
                    HttpMethod.valueOf(request.method()),
                    entity,
                    String.class
            );

            return new SpeedyRawResponse(
                    response.getStatusCode().value(),
                    response.getHeaders(),
                    response.getBody()
            );
        } catch (HttpStatusCodeException e) {
            // Don't let RestTemplate throw — we handle status codes ourselves
            return new SpeedyRawResponse(
                    e.getStatusCode().value(),
                    e.getResponseHeaders() != null ? e.getResponseHeaders() : new HttpHeaders(),
                    e.getResponseBodyAsString()
            );
        }
    }
}
```

### MockMvcTransport — Test Adapter

```java
package com.github.silent.samurai.speedy.client.test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * SpeedyTransport implementation using Spring MockMvc.
 * Returns raw HTTP response — SpeedyTest handles assertions.
 */
class MockMvcTransport implements SpeedyTransport {

    private final MockMvc mockMvc;

    MockMvcTransport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Override
    public SpeedyRawResponse send(SpeedyRequest request) throws IOException {
        try {
            MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .request(request.method(), URI.create(request.url()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

            request.headers().forEach((name, values) ->
                values.forEach(v -> builder.header(name, v)));

            if (request.body() != null) {
                builder.content(request.body());
            }

            MvcResult result = mockMvc.perform(builder).andReturn();

            Map<String, List<String>> headers = new HashMap<>();
            result.getResponse().getHeaderNames().forEach(name ->
                headers.put(name, List.of(result.getResponse().getHeader(name))));

            return new SpeedyRawResponse(
                result.getResponse().getStatus(),
                headers,
                result.getResponse().getContentAsString()
            );
        } catch (Exception e) {
            throw new IOException("MockMvc request failed", e);
        }
    }
}
```

---

## Migration Guide

### Before (Current API)

```java
// Setup
MockMvcClientHttpRequestFactory factory = new MockMvcClientHttpRequestFactory(mvc);
RestTemplate restTemplate = new RestTemplate(factory);
SpeedyClient<SpeedyResponse> client = SpeedyClient.restTemplate(restTemplate, "http://localhost");

// Create
SpeedyResponse resp = client.create("Category")
    .addField("name", "cat-1")
    .execute();
assertFalse(resp.getPayload().isEmpty());
JsonNode entity = resp.getPayload().get(0);
String id = entity.get("id").asText();

// Query
SpeedyResponse queryResp = client.query(
    SpeedyQuery.from("Category")
        .where(condition("name", eq("cat-1")))
).execute();
List<Category> categories = queryResp.asList(Category.class);

// Count (BROKEN in current design)
SpeedyResponse countResp = client.count(
    SpeedyQuery.from("Category")
        .where(condition("active", eq(true)))
);
long count = countResp.asCount(); // Always returns 0!

// Error handling
assertThrows(HttpClientErrorException.BadRequest.class, () -> {
    client.create("User").addField("email", "invalid").execute();
});
```

### After (New API)

```java
// Setup
Speedy speedy = Speedy.builder()
                .baseUrl("http://localhost:8080")
                .transport(new RestTemplateTransport(restTemplate))
                .build();

// Create
String id = speedy.create("Category")
        .field("name", "cat-1")
        .execute()
        .first(Category.class)
        .getId();

// Query
List<Category> categories = speedy.query("Category")
        .where(condition("name", eq("cat-1")))
        .execute()
        .list(Category.class);

// Count (WORKS correctly)
long count = speedy.query("Category")
        .where(condition("active", eq(true)))
        .count();

// Error handling
SpeedyBadRequestException ex = assertThrows(SpeedyBadRequestException.class, () -> {
    speedy.create("User").field("email", "invalid").execute();
});

assertEquals(400,ex.statusCode());

assertTrue(ex.serverMessage().

contains("email"));

// ─── Test mode ──────────────────────────────────────────────────────

SpeedyTest test = SpeedyTest.mockMvc(mockMvc);

test.

create("User")
    .

field("email","invalid")
    .

execute()
    .

expectBadRequest()
    .

expectJsonPath("$.message",containsString("email"));
```

---

## Design Decisions & Rationale

### Why remove the `<T>` generic?

| Generic `<T>`                   | Problems                                                              |
|---------------------------------|-----------------------------------------------------------------------|
| `SpeedyClient<SpeedyResponse>`  | Leaks transport type into all builder signatures                      |
| `SpeedyClient<ResultActions>`   | Incompatible return types — can't call `.asList()` on `ResultActions` |
| `SpeedyCreateRequestBuilder<T>` | Generic noise provides zero type safety for the entity being created  |

The generic exists solely to return different types from `execute()`. The new design solves this by having **two
separate classes** (`Speedy` and `SpeedyTest`) that share builders internally but present different result types.

### Why unchecked exceptions?

1. Every current method declares `throws Exception` — provides zero useful information
2. The server has 3 error cases (400, 404, 500) — all are representable as specific unchecked exceptions
3. Checked exceptions force try-catch at every call site even when the error is unrecoverable
4. Matches industry standard (Retrofit, AWS SDK, Stripe SDK all use unchecked)

### Why separate `Speedy` and `SpeedyTest`?

| Concern          | `Speedy`                            | `SpeedyTest`                                |
|------------------|-------------------------------------|---------------------------------------------|
| Return type      | `SpeedyResult` (typed data)         | `SpeedyTestResult` (assertions)             |
| Error handling   | Throws `SpeedyException`            | Returns result with status for assertion    |
| Use case         | Production code, service-to-service | Integration tests                           |
| Response parsing | Full deserialization                | Optionally lazy — assert first, parse later |

Trying to unify these into one class with `<T>` creates friction for both use cases.

### Why framework-agnostic core?

1. `SpeedyTransport` uses only `String` + `Map` + `int` — no Spring types
2. Default `JdkHttpTransport` works without any framework
3. Spring adapter is optional — add `spring-web` to classpath and use `RestTemplateTransport`
4. Enables future adapters: OkHttp, Apache HttpClient, Vert.x, etc.
5. Removes the forced transitive dependency on Spring for library consumers

### Why single ObjectMapper?

Current code has 3 instances with different configs. The redesign uses ONE mapper configured at
`Speedy.builder().objectMapper(...)` time, defaulting to:

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
```

This single instance flows through all builders, response parsing, and field conversion.

---

## Implementation Order

| Phase  | Task                                                                                          | Effort   |
|--------|-----------------------------------------------------------------------------------------------|----------|
| **1**  | Transport layer (`SpeedyTransport`, `SpeedyRequest`, `SpeedyRawResponse`, `JdkHttpTransport`) | 1 day    |
| **2**  | Exception hierarchy (`SpeedyException` + subclasses)                                          | 0.5 day  |
| **3**  | `ResponseParser` (central parsing logic)                                                      | 0.5 day  |
| **4**  | Internal utilities (`PathBuilder`, `FieldUtil`)                                               | 0.5 day  |
| **5**  | `SpeedyResult` (typed response wrapper)                                                       | 0.5 day  |
| **6**  | `SpeedyQuery` (port operators, keep DSL unchanged)                                            | 0.5 day  |
| **7**  | Builders (`CreateBuilder`, `GetBuilder`, `UpdateBuilder`, `DeleteBuilder`, `QueryBuilder`)    | 1 day    |
| **8**  | `Speedy` class (main facade + builder)                                                        | 0.5 day  |
| **9**  | `RestTemplateTransport` (Spring adapter)                                                      | 0.5 day  |
| **10** | `SpeedyTest` + `SpeedyTestResult` + `MockMvcTransport`                                        | 1 day    |
| **11** | Unit tests for all new classes                                                                | 1.5 days |
| **12** | Migrate integration tests in `speedy-test-app`                                                | 1 day    |
| **13** | Remove old code, update POM, update README                                                    | 0.5 day  |

**Total estimated effort: ~9 days**

---

## What Stays Unchanged

1. **Query DSL operators** — `eq()`, `ne()`, `gt()`, `condition()`, `and()`, `or()` keep the same signatures
2. **Dot-notation for nested fields** — `field("category.id", "1")` works the same
3. **Builder pattern** — `create().field().execute()` fluent chain preserved
4. **Server API contract** — No server-side changes needed
5. **Static import pattern** — `import static ...SpeedyQuery.*` still recommended

---

## Comparison With Industry SDKs

| Feature         | Speedy (New)        | Stripe Java            | AWS SDK v2             | Supabase-js     |
|-----------------|---------------------|------------------------|------------------------|-----------------|
| Typed responses | `first(T.class)`    | Auto-typed             | `response.items()`     | `.returns<T>()` |
| Error hierarchy | 3 exception classes | `StripeException` tree | `AwsServiceException`  | Error objects   |
| Interceptors    | `SpeedyInterceptor` | `RequestOptions`       | `ExecutionInterceptor` | Headers config  |
| Transport SPI   | `SpeedyTransport`   | `HttpClient`           | `SdkHttpClient`        | Fetch adapter   |
| Builder pattern | Fluent              | Params objects         | Request builders       | Method chaining |
| Framework deps  | None (Jackson only) | None (Gson)            | None                   | None            |
| Test support    | `SpeedyTest`        | Mock server            | Mock client            | Mock fetch      |
