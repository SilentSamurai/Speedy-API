# Speedy Java Client

A library-agnostic Java client for Speedy API. Zero framework dependency on the default classpath — bring your own HTTP transport.

## Quick Start

### Production (JDK HttpClient, zero deps)

```java
Speedy speedy = Speedy.connect("http://localhost:8080");

List<User> users = speedy.get("User")
    .execute()
    .list(User.class);
```

### Spring RestTemplate (optional)

```java
Speedy speedy = Speedy.builder()
    .baseUrl("http://localhost:8080")
    .transport(new RestTemplateTransport(new RestTemplate()))
    .build();

User user = speedy.get("User")
    .key("id", 123)
    .execute()
    .first(User.class);
```

### MockMvc Integration Testing

```java
SpeedyTest speedy = SpeedyTest.mockMvc(mockMvc);

speedy.get("User")
    .key("id", 123)
    .execute()
    .expectOk()
    .expectJsonPath("$.payload[*].name", everyItem(notNullValue()));
```

## CRUD Operations

```java
import static com.github.silent.samurai.speedy.client.SpeedyQuery.*;

// Create
SpeedyResult created = speedy.create("User")
    .field("name", "John")
    .field("email", "john@example.com")
    .execute();
User user = created.first(User.class);

// Read by PK
User user = speedy.get("User")
    .key("id", 123)
    .execute()
    .first(User.class);

// Update
speedy.update("User")
    .key("id", 123)
    .field("name", "Jane")
    .execute();

// Delete
speedy.delete("User")
    .key("id", 123)
    .execute();
```

## Query Building

```java
import static com.github.silent.samurai.speedy.client.SpeedyQuery.*;

List<User> users = speedy.query("User")
    .where(
        and(
            condition("active", eq(true)),
            condition("age", gte(18))
        )
    )
    .orderByAsc("name")
    .pageSize(20)
    .pageNo(0)
    .expand("profile")
    .select("id", "name", "email")
    .execute()
    .list(User.class);

// Count
long total = speedy.query("User")
    .where(condition("active", eq(true)))
    .count();
```

## Typed Responses

`SpeedyResult` provides typed deserialization:

```java
SpeedyResult result = speedy.get("User").execute();

List<User> users = result.list(User.class);
User first = result.first(User.class);
Optional<User> opt = result.firstOptional(User.class);
ArrayNode raw = result.raw();
JsonNode firstRaw = result.firstRaw();
int page = result.pageIndex();
int size = result.pageSize();
boolean empty = result.isEmpty();
```

## Error Handling

All errors surface as unchecked `SpeedyException` subclasses:

```java
try {
    speedy.create("User").field("name", null).execute();
} catch (SpeedyBadRequestException e) {
    e.getStatusCode();   // 400
    e.getServerMessage(); // validation error detail
}
```

| Exception | HTTP Status |
|-----------|-------------|
| `SpeedyBadRequestException` | 400 |
| `SpeedyNotFoundException` | 404 |
| `SpeedyServerException` | 500+ |
| `SpeedyConnectionException` | network failure |
| `SpeedyDeserializationException` | JSON parse failure |

## Custom Transport

Implement `SpeedyTransport` for any HTTP library:

```java
Speedy speedy = Speedy.builder()
    .baseUrl("http://localhost:8080")
    .transport(request -> {
        // Your custom HTTP logic
        return new SpeedyRawResponse(200, Map.of(), "{\"payload\":[]}");
    })
    .build();
```

## Interceptors

Chain interceptors for auth headers, logging, tracing:

```java
Speedy speedy = Speedy.builder()
    .baseUrl("http://localhost:8080")
    .interceptor(req -> req.withHeader("Authorization", "Bearer token"))
    .interceptor(req -> req.withHeader("X-Trace-Id", UUID.randomUUID().toString()))
    .build();
```

## Query Operators

| Operator | Method |
|----------|--------|
| `$eq` | `eq(value)` |
| `$ne` | `ne(value)` |
| `$gt` | `gt(value)` |
| `$lt` | `lt(value)` |
| `$gte` | `gte(value)` |
| `$lte` | `lte(value)` |
| `$in` | `in(values...)` |
| `$nin` | `nin(values...)` |
| `$matches` | `matches(value)` |
| `$contains` | `contains(value)` |

Logical: `and(conditions...)`, `or(conditions...)`

## Dependencies

- **Required**: Jackson (`jackson-databind`, `jackson-datatype-jsr310`)
- **Optional**: Spring Web (`RestTemplateTransport`), Spring Test (`MockMvcTransport`), Hamcrest + json-path (`SpeedyTestResult` assertions)
- **No** Spring, Lombok, or `speedy-commons` on default classpath
