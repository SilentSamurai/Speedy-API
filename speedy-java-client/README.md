# Speedy Java Client

A library-agnostic Java client for Speedy API that allows you to use any HTTP client implementation of your choice.

## Quick Start

### RestTemplate (Production)

```java
RestTemplate restTemplate = new RestTemplate();
SpeedyClient<SpeedyResponse> client = SpeedyClient.restTemplate(restTemplate, "http://localhost:8080");

SpeedyResponse response = client.get("users")
    .key("id", 123)
    .execute();

List<User> users = response.asList(User.class);
```

### MockMvc (Testing)

```java
SpeedyClient<ResultActions> client = SpeedyClient.mockMvc(mockMvc);

ResultActions result = client.create("users")
    .field("name", "Test User")
    .execute();

result.andExpect(status().isCreated());
```

## Query Building

Build complex queries using the fluent API with static imports:

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

SpeedyQuery query = from("users")
    .where(
        and(
            condition("active", eq(true)),
            condition("age", gte(18))
        )
    )
    .orderByAsc("name")
    .pageSize(10)
    .pageNo(0)
    .expand("profile")
    .select("id", "name", "email");

SpeedyResponse response = client.query(query).execute();
List<User> users = response.asList(User.class);
```

## CRUD Operations

### Create

```java
// Single entity
SpeedyResponse response = client.create("User")
    .field("name", "John Doe")
    .field("email", "john@example.com")
    .execute();

// Bulk create
ArrayNode entities = mapper.createArrayNode();
entities.add(mapper.createObjectNode().put("name", "Alice"));
entities.add(mapper.createObjectNode().put("name", "Bob"));
SpeedyResponse response = client.createMany("User", entities);
```

### Read

```java
// By primary key
SpeedyResponse response = client.get("User")
    .key("id", 123)
    .execute();

User user = response.asSingle(User.class);

// List all
SpeedyResponse response = client.get("User").execute();
List<User> users = response.asList(User.class);
```

### Update

```java
SpeedyResponse response = client.update("User")
    .key("id", 123)
    .field("name", "Jane Doe")
    .field("email", "jane@example.com")
    .execute();
```

### Delete

```java
// Single
SpeedyResponse response = client.delete("User")
    .key("id", 123)
    .execute();

// Bulk
ArrayNode pks = mapper.createArrayNode();
pks.add(mapper.createObjectNode().put("id", 123));
pks.add(mapper.createObjectNode().put("id", 456));
SpeedyResponse response = client.deleteMany("User", pks);
```

## Count Queries

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

SpeedyQuery query = from("users")
    .where(condition("active", eq(true)))
    .build();

SpeedyResponse response = client.count(query);
long total = response.asCount();
```

## Metadata

```java
SpeedyResponse metadata = client.metadata();
```

## Typed Responses

`SpeedyResponse` provides helper methods for automatic deserialization:

```java
SpeedyResponse response = client.get("users").execute();

// Deserialize payload as a list
List<User> users = response.asList(User.class);

// Deserialize first element
User user = response.asSingle(User.class);

// Extract count from count queries
long total = response.asCount();

// Access pagination metadata
int page = response.getPageIndex();
int size = response.getPageSize();
int totalPages = response.getTotalPageCount();
```

## Custom API Path

```java
SpeedyClient<SpeedyResponse> client = SpeedyClient.restTemplate(restTemplate, "http://localhost:8080")
    .baseUrl("/custom/path/");
```

## Custom HTTP Client

```java
public class OkHttpClientImpl implements HttpClient<MyResponse> {
    // Implement methods using OkHttp
}

SpeedyClient<MyResponse> client = SpeedyClient.from(new OkHttpClientImpl());
```

## Building Requests Without Executing

All builders expose `build()` for inspection or deferred execution:

```java
SpeedyGetRequest request = client.get("users")
    .key("id", 123)
    .build();

// Inspect or modify the request
logger.info("Entity: {}", request.getEntity());
logger.info("PK: {}", request.getPk());

// Execute later
SpeedyResponse response = client.get(request).execute();
```

## Available Query Operators

| Operator | Method | Description |
|----------|--------|-------------|
| `$eq` | `eq(value)` | Equal to |
| `$ne` | `ne(value)` | Not equal to |
| `$gt` | `gt(value)` | Greater than |
| `$lt` | `lt(value)` | Less than |
| `$gte` | `gte(value)` | Greater than or equal |
| `$lte` | `lte(value)` | Less than or equal |
| `$in` | `in(values...)` | In array of values |
| `$nin` | `nin(values...)` | Not in array |
| `$matches` | `matches(value)` | Pattern matching |
| `$contains` | `contains(value)` | Substring/collection containment |

## Logical Operators

| Operator | Method | Description |
|----------|--------|-------------|
| `$and` | `and(conditions...)` | Logical AND |
| `$or` | `or(conditions...)` | Logical OR |

## Dependencies

- Jackson for JSON processing (via `speedy-commons`)
- Spring Web types (for the default RestTemplate implementation)
- Spring Test (optional, for MockMvc testing)
- Lombok (provided scope)
