# Java Client

The Speedy Java Client provides a fluent, type-safe API for interacting with Speedy-enabled backends. It offers a clean, builder-based interface for making CRUD operations and complex queries.

## Features

- **Fluent API**: Chain methods for building complex requests
- **Type Safety**: Compile-time validation of request structure
- **Multiple HTTP Clients**: Support for RestTemplate, MockMvc, and custom implementations
- **Query Builder**: Powerful query construction with conditions, ordering, and pagination
- **Testing Support**: Built-in MockMvc client for unit and integration testing

## Maven Dependency

```xml
<dependency>
    <groupId>com.github.SilentSamurai</groupId>
    <artifactId>speedy-java-client</artifactId>
    <version>3.1.0</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

// Create a client
RestTemplate restTemplate = new RestTemplate();
SpeedyClient<SpeedyResponse> client = SpeedyClient.restTemplate(restTemplate, "http://localhost:8080");

// Create a new category
SpeedyResponse response = client.create("Category")
    .addField("name", "cat-client-1")
    .execute();

// Query categories
SpeedyQuery query = SpeedyQuery.from("Category")
    .where(condition("name", eq("cat-client-1")))
    .build();

SpeedyResponse categories = client.query(query).execute();
```

### Testing with MockMvc

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCreateCategory() {
        SpeedyClient<ResultActions> client = SpeedyClient.mockMvc(mockMvc);
        
        ResultActions result = client.create("Category")
            .addField("name", "test-category")
            .execute();
            
        result.andExpect(status().isCreated());
    }
}
```

## Components

### [SpeedyClient](speedy-client.md)
The main client class that provides CRUD operations and query execution.

### [SpeedyQuery](speedy-query.md)
A fluent query builder for constructing complex database queries with conditions, ordering, and pagination.

## HTTP Client Support

The Java client supports multiple HTTP client implementations:

| Client | Use Case | Factory Method |
|--------|----------|----------------|
| **RestTemplate** | Production applications | `SpeedyClient.restTemplate(restTemplate, baseUrl)` |
| **MockMvc** | Unit/integration testing | `SpeedyClient.mockMvc(mockMvc)` |
| **Custom** | Custom implementations | `SpeedyClient.from(httpClient)` |

## Best Practices

1. **Use static imports** for cleaner code:
   ```java
   import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
   ```

2. **Handle responses properly**:
   ```java
   SpeedyResponse response = client.get("users").execute();
   if (response.isSuccess()) {
       List<User> users = response.getData();
   } else {
       // Handle error appropriately
   }
   ```

3. **Use appropriate HTTP clients**:
   - Use `RestTemplate` for production
   - Use `MockMvc` for testing
   - Create custom clients for special requirements

4. **Optimize queries**:
   - Select only needed fields
   - Use appropriate page sizes
   - Add proper conditions to reduce data transfer

## Error Handling

The client provides comprehensive error handling:

```java
try {
    SpeedyResponse response = client.create("users")
        .addField("name", "John Doe")
        .addField("email", "john@example.com")
        .execute();
} catch (SpeedyClientException e) {
    // Handle client-specific errors
    log.error("Client error: {}", e.getMessage());
} catch (Exception e) {
    // Handle general errors
    log.error("Unexpected error: {}", e.getMessage());
}
```