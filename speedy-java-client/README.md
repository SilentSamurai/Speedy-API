# Speedy Java Client

A library-agnostic Java client for Speedy API that allows you to use any HTTP client implementation of your choice.

## Architecture

The Speedy Java Client now uses an interface-based approach that allows you to plug in your own HTTP client
implementation:

- **`HttpClient` interface**: Defines the contract for HTTP operations
- **`ApiClient` class**: Default implementation using Spring's RestTemplate
- **`SpeedyApi` class**: Main API client that uses the HttpClient interface
- **Query builders**: Fluent builders for creating complex queries

## Usage Options

### Option 1: Use with Default HTTP Client (Spring RestTemplate)

```java
// Using default ApiClient with RestTemplate
SpeedyApi speedyApi = new SpeedyApi();

// Or with custom RestTemplate
RestTemplate customRestTemplate = new RestTemplate();
SpeedyApi speedyApi = new SpeedyApi(customRestTemplate);
```

### Option 2: Use with Your Own HTTP Client

```java
// Implement the HttpClient interface with your preferred HTTP library
public class MyCustomHttpClient implements HttpClient {
    // Implement using OkHttp, Apache HttpClient, etc.
    @Override
    public <T> ResponseEntity<T> invokeAPI(...) {
        // Your HTTP client implementation here
    }
    
    // Implement other methods...
}

// Use your custom HTTP client
SpeedyApi speedyApi = new SpeedyApi(new MyCustomHttpClient());
```

### Option 3: Use Only Request/Response Generation (No HTTP Client)

```java
// Use static builder methods for just building requests
SpeedyQuery query = SpeedyQuery.builder("User")
    .where(SpeedyQuery.condition("email", SpeedyQuery.eq("john@example.com")))
    .orderByAsc("name")
    .pageSize(10);

JsonNode requestBody = query.build();
// Use requestBody with your own HTTP client
```

## Query Building

Build complex queries using the fluent API:

```java
SpeedyQuery query = SpeedyQuery.builder("User")
    .where(
        SpeedyQuery.and(
            SpeedyQuery.condition("email", SpeedyQuery.eq("john@example.com")),
            SpeedyQuery.condition("age", SpeedyQuery.gt(18))
        )
    )
    .orderByAsc("name")
    .pageSize(10)
    .pageNo(0)
    .expand("profile")
    .select("id", "name", "email");

// Execute the query
SpeedyResponse response = speedyApi.query(query);
```

## CRUD Operations

```java
// Create
SpeedyCreateRequest createRequest = SpeedyCreateRequest.builder("User")
    .addField("name", "John Doe")
    .addField("email", "john@example.com")
    .build();
SpeedyResponse response = speedyApi.create(createRequest);

// Read
SpeedyGetRequest getRequest = SpeedyGetRequest.builder("User")
    .pk("id", "123")
    .build();
SpeedyResponse response = speedyApi.get(getRequest);

// Update
SpeedyUpdateRequest updateRequest = SpeedyUpdateRequest.builder("User")
    .addField("name", "Jane Doe")
    .where("id", "123")
    .build();
SpeedyResponse response = speedyApi.update(updateRequest);

// Delete
SpeedyDeleteRequest deleteRequest = SpeedyDeleteRequest.builder("User")
    .pk("id", "123")
    .build();
SpeedyResponse response = speedyApi.delete(deleteRequest);
```

## Benefits

1. **Library Agnostic**: Use any HTTP client library (OkHttp, Apache HttpClient, etc.)
2. **Backward Compatible**: Existing code using RestTemplate continues to work
3. **Flexible**: Choose between full HTTP client integration or just request/response generation
4. **Type Safe**: Strong typing with fluent builders
5. **Easy Testing**: Mock the HttpClient interface for unit tests

## Dependencies

The client has minimal dependencies:

- Jackson for JSON processing
- Jakarta Validation for annotations
- Lombok for reducing boilerplate (optional)
- Spring HTTP types (for the default implementation only)

## Custom HTTP Client Example

See `CustomHttpClientExample.java` for a complete example of implementing your own HTTP client. 