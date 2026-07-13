# Java Client

The `speedy-client` module is a library-agnostic Java client for calling a Speedy-enabled backend. It has **no
required dependency on Spring, Lombok, or `speedy-commons`** — only Jackson. Bring your own HTTP transport, or use
the zero-dependency JDK `HttpClient` transport out of the box.

## Features

- **Fluent API** — chainable builders for create, get, update, replace, delete, and query
- **Typed responses** — deserialize results directly into your own POJOs via `SpeedyResult`
- **Pluggable transport** — JDK `HttpClient` (default), Spring `RestTemplate`, `MockMvc`, or any custom
  `SpeedyTransport`
- **Typed exceptions** — `SpeedyException` subclasses map to HTTP status categories, no leaked framework exceptions
- **Interceptors** — chain request interceptors for auth headers, tracing, logging
- **Testing support** — `SpeedyTest`, a MockMvc-backed facade with built-in JSON-path assertions

## Maven Dependency

```xml
<dependency>
    <groupId>com.github.silentsamurai</groupId>
    <artifactId>speedy-client</artifactId>
    <version>3.1.4</version>
</dependency>
```

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
// Create
SpeedyResult created = speedy.create("User")
        .field("name", "John")
        .field("email", "john@example.com")
        .execute();
User user = created.first(User.class);

// Read by primary key
User user = speedy.get("User")
        .key("id", 123)
        .execute()
        .first(User.class);

// Update (PATCH — partial)
speedy.update("User")
        .key("id", 123)
        .field("name", "Jane")
        .execute();

// Replace (PUT — full replace)
speedy.replace("User")
        .key("id", 123)
        .field("name", "Jane")
        .field("email", "jane@example.com")
        .execute();

// Delete
speedy.delete("User")
        .key("id", 123)
        .execute();
```

`field(name, value)` supports dot-notation for foreign-key fields, e.g. `field("category.id", categoryId)`.

Bulk variants are available for all mutating operations: `speedy.createMany(...)`, `speedy.updateMany(...)`,
`speedy.replaceMany(...)`, `speedy.deleteMany(...)` — each accepting a `List<ObjectNode>` or returning a dedicated
bulk builder (`BulkCreateBuilder`, `BulkUpdateBuilder`, `BulkReplaceBuilder`, `BulkDeleteBuilder`).

See [SpeedyClient](speedy-client.md) for the full builder reference (including error handling, transports, and
interceptors) and [SpeedyQuery](speedy-query.md) for the query/condition DSL.

## GET List Queries

`get()` without a primary key returns a list, and supports field selection, pagination, and expansion as URL query
parameters:

```java
Speedy speedy = Speedy.connect("http://localhost:8080");

// Select specific fields
List<Product> products = speedy.get("Product")
        .select("id", "name", "description")
        .execute()
        .list(Product.class);

// Select + pagination + expansion
List<Product> products = speedy.get("Product")
        .select("id", "name")
        .pageSize(20)
        .pageNo(0)
        .expand("category")
        .execute()
        .list(Product.class);
```

These methods produce URL query parameters (`$select=id,name&$pageSize=20&...`) rather than JSON body fields,
matching the [GET Operation](get-operation.md) API. For conditions, ordering, and count queries, use
`speedy.query(...)` — see [SpeedyQuery](speedy-query.md).

## Character Encoding

All transports guarantee UTF-8 encoding for both request and response:

- `Content-Type: application/json;charset=UTF-8` on requests with a body
- `Accept: application/json;charset=UTF-8` on all requests
- `MockMvcTransport` sets `characterEncoding=UTF-8` on the request builder
- `RestTemplateTransport` configures `StringHttpMessageConverter(StandardCharsets.UTF_8)`
- `JdkHttpTransport` uses `BodyPublishers.ofString(body, StandardCharsets.UTF_8)`

Response bodies are always decoded as UTF-8.

## Wire Format

JSON is the default wire format. XML and YAML are also supported via `speedy-io-xml` / `speedy-io-yaml` on the
server side; configure the client to match with `Speedy.builder().format(new XmlFormat())` or
`.format(new YamlFormat())`.

## Transports

| Transport             | Use Case                            |
|------------------------|-------------------------------------|
| `JdkHttpTransport`     | Default — zero extra dependencies   |
| `RestTemplateTransport`| Spring applications already using `RestTemplate` |
| `MockMvcTransport`     | Integration tests (used internally by `SpeedyTest`) |
| Custom `SpeedyTransport` | Any other HTTP library             |

See [SpeedyClient](speedy-client.md) for custom transport and interceptor examples.
