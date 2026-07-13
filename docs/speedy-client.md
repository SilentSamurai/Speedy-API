# SpeedyClient

`Speedy` is the main entry point for the production Java client. It's configured once (via `Speedy.connect(...)` or
`Speedy.builder()...build()`) and reused for all calls — each call returns a fluent builder for one operation.

## Creating a Client

### Quick Connect (JDK HttpClient, zero deps)

```java
Speedy speedy = Speedy.connect("http://localhost:8080");
```

### Using RestTemplate

```java
Speedy speedy = Speedy.builder()
        .baseUrl("http://localhost:8080")
        .transport(new RestTemplateTransport(new RestTemplate()))
        .build();
```

### Custom Transport

Implement `SpeedyTransport` for any HTTP library — it's a single-method functional interface mapping a
`SpeedyRequest` to a `SpeedyRawResponse`:

```java
Speedy speedy = Speedy.builder()
        .baseUrl("http://localhost:8080")
        .transport(request -> {
            // Your custom HTTP logic
            return new SpeedyRawResponse(200, Map.of(), "{\"payload\":[]}");
        })
        .build();
```

### Testing with MockMvc

`SpeedyTest` mirrors the same builder API but returns `SpeedyTestResult`, which carries the status code for
assertions instead of throwing on 4xx/5xx:

```java
@Autowired
private MockMvc mockMvc;

SpeedyTest speedy = SpeedyTest.mockMvc(mockMvc);
```

## CRUD Operations

### Create

```java
SpeedyResult result = speedy.create("Category")
        .field("name", "cat-client-1")
        .execute();
Category created = result.first(Category.class);

// Foreign-key fields via dot notation
speedy.create("Product")
        .field("name", "client-product-1")
        .field("description", "test description")
        .field("category.id", categoryId)
        .execute();
```

### Read

```java
// By primary key
User user = speedy.get("User")
        .key("id", userId)
        .execute()
        .first(User.class);

// List, with selection/pagination/expansion
List<User> users = speedy.get("User")
        .select("id", "name", "email")
        .pageSize(20)
        .pageNo(0)
        .expand("profile")
        .execute()
        .list(User.class);
```

### Update — PATCH (partial)

```java
speedy.update("Category")
        .key("id", categoryId)
        .field("name", "cat-updated")
        .execute();
```

Only the fields set via `field(...)` are sent; the server leaves every other column unchanged. See
[PUT Operations](put-operation.md) for PATCH vs. PUT semantics.

### Replace — PUT (full replace)

```java
speedy.replace("Category")
        .key("id", categoryId)
        .field("name", "cat-updated")
        .field("description", "full replacement value")
        .execute();
```

`replace(...)` sends every field as the complete representation of the resource: omitted nullable fields are reset
to `null` on the server, and omitted required fields are rejected with `400`.

### Delete

```java
speedy.delete("Category")
        .key("id", categoryId)
        .execute();
```

## Bulk Operations

Every mutating operation has a bulk counterpart, each taking a `List<ObjectNode>` (or its own item-by-item builder):

```java
// Bulk create
SpeedyResult result = speedy.createMany("Category", List.of(categoryA, categoryB));

// Or via the item builder
speedy.createMany("Category")
        .items(List.of(categoryA, categoryB))
        .execute();

// Bulk update / replace / delete follow the same shape
speedy.updateMany("Category", items);
speedy.replaceMany("Category", items);
speedy.deleteMany("Category", primaryKeys);
```

## Query Operations

`speedy.query(entity)` builds an advanced query against `POST /{Entity}/$query` — see [SpeedyQuery](speedy-query.md)
for the full condition/operator DSL:

```java
import static com.github.silent.samurai.speedy.client.SpeedyQuery.*;

List<Category> categories = speedy.query("Category")
        .where(condition("name", eq("cat-updated")))
        .orderByAsc("name")
        .execute()
        .list(Category.class);

// Count
long total = speedy.query("Category")
        .where(condition("active", eq(true)))
        .count();
```

## Response Handling

`SpeedyResult` wraps the response payload and pagination metadata, with typed deserialization helpers:

```java
SpeedyResult result = speedy.get("User").execute();

List<User> users = result.list(User.class);
User first = result.first(User.class);              // null if empty
Optional<User> opt = result.firstOptional(User.class);
JsonNode raw = result.raw();                          // raw payload array
JsonNode firstRaw = result.firstRaw();
int pageIndex = result.pageIndex();
int pageSize = result.pageSize();
long totalCount = result.totalCount();
int totalPages = result.totalPages();
boolean empty = result.isEmpty();
int size = result.size();
```

## Error Handling

All errors surface as unchecked `SpeedyException` subclasses — no framework exceptions leak through:

```java
try {
    speedy.create("User").field("name", null).execute();
} catch (SpeedyBadRequestException e) {
    e.statusCode();      // 400
    e.serverMessage();   // validation error detail from the server
    e.timestamp();
    e.responseBody();    // raw response body, for debugging
}
```

| Exception                        | HTTP Status        |
|-----------------------------------|--------------------|
| `SpeedyBadRequestException`       | 400                |
| `SpeedyNotFoundException`         | 404                |
| `SpeedyServerException`           | 500+                |
| `SpeedyConnectionException`       | network failure     |
| `SpeedyDeserializationException`  | JSON parse failure  |

All of the above extend `SpeedyException`, so a single `catch (SpeedyException e)` handles every case if you don't
need to distinguish them.

## Interceptors

Chain interceptors for auth headers, logging, or tracing — each receives and can transform the outgoing
`SpeedyRequest`:

```java
Speedy speedy = Speedy.builder()
        .baseUrl("http://localhost:8080")
        .interceptor(req -> req.withHeader("Authorization", "Bearer token"))
        .interceptor(req -> req.withHeader("X-Trace-Id", UUID.randomUUID().toString()))
        .build();
```

## Testing with SpeedyTest

`SpeedyTest` exposes the same `create`/`get`/`update`/`replace`/`delete`/`query` methods as `Speedy`, backed by
`MockMvc` instead of a network transport. Results are `SpeedyTestResult`, which adds fluent assertions instead of
throwing on error responses:

```java
@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCreateCategory() {
        SpeedyTest speedy = SpeedyTest.mockMvc(mockMvc);

        speedy.create("Category")
                .field("name", "test-category")
                .execute()
                .expectCreated()
                .expectJsonPath("$.payload[0].name", "test-category");
    }
}
```

| Assertion                                    | Checks                                  |
|-----------------------------------------------|------------------------------------------|
| `expectStatus(int)`                          | Exact HTTP status                        |
| `expectOk()`                                 | 200                                       |
| `expectCreated()`                             | 201                                       |
| `expectBadRequest()`                          | 400                                       |
| `expectNotFound()`                            | 404                                       |
| `expectNotModified()`                         | 304                                       |
| `expectPreconditionFailed()`                  | 412                                       |
| `expectJsonPath(expr, matcher\|value)`        | JSON-path value/Hamcrest matcher          |
| `expectJsonPathExists(expr)`                  | JSON-path is present                     |
| `expectJsonPathDoesNotExist(expr)`            | JSON-path is absent                      |

## Factory / Entry-Point Methods Reference

| Method                            | Returns              | Purpose                                  |
|------------------------------------|-----------------------|-------------------------------------------|
| `Speedy.connect(baseUrl)`          | `Speedy`              | Quick-connect with JDK transport defaults |
| `Speedy.builder()...build()`       | `Speedy`              | Full configuration (transport, interceptors, format) |
| `SpeedyTest.mockMvc(mockMvc)`      | `SpeedyTest`          | MockMvc-backed test facade                |
| `speedy.create(entity)`            | `CreateBuilder`       | Create one entity                         |
| `speedy.get(entity)`               | `GetBuilder`          | Fetch by key, or list                     |
| `speedy.update(entity)`            | `UpdateBuilder`       | PATCH — partial update                    |
| `speedy.replace(entity)`           | `ReplaceBuilder`      | PUT — full replace                        |
| `speedy.delete(entity)`            | `DeleteBuilder`       | Delete by key                             |
| `speedy.query(entity)`             | `QueryBuilder`        | Advanced query / count                    |
| `speedy.createMany(entity)`        | `BulkCreateBuilder`   | Bulk create                               |
| `speedy.updateMany(entity)`        | `BulkUpdateBuilder`   | Bulk update                               |
| `speedy.replaceMany(entity)`       | `BulkReplaceBuilder`  | Bulk replace                              |
| `speedy.deleteMany(entity)`        | `BulkDeleteBuilder`   | Bulk delete                               |
| `speedy.metadata()`                | `JsonNode`            | Fetches `/$metadata`                      |
