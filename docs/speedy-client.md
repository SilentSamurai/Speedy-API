# SpeedyClient

The `SpeedyClient<T>` is a generic client class that provides a fluent API for interacting with the Speedy API. It serves as the main entry point for making CRUD operations and queries against a Speedy-enabled backend.

## Overview

The SpeedyClient offers:
- **Generic Type Support**: Uses type parameter `T` to handle different response types
- **Builder Pattern**: Provides builder classes for different HTTP operations
- **HTTP Client Abstraction**: Supports different HTTP client implementations
- **Query Support**: Allows execution of custom queries using `SpeedyQuery`

## Creating a Client

### Using RestTemplate (Production)

```java
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import org.springframework.web.client.RestTemplate;

RestTemplate restTemplate = new RestTemplate();
SpeedyClient<SpeedyResponse> client = SpeedyClient.restTemplate(restTemplate, "https://api.example.com");
```

### Using MockMvc (Testing)

```java
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Autowired
private MockMvc mockMvc;

SpeedyClient<ResultActions> testClient = SpeedyClient.mockMvc(mockMvc);
```

### Using Custom HTTP Client

```java
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import com.github.silent.samurai.speedy.api.client.HttpClient;

HttpClient<CustomResponse> customClient = new CustomHttpClient();
SpeedyClient<CustomResponse> client = SpeedyClient.from(customClient);
```

## CRUD Operations

### Create Operations

```java
// Create a single user with fields
SpeedyResponse response = client.create("Category")
    .addField("name", "cat-client-1")
    .execute();

// Create with multiple fields
SpeedyResponse response = client.create("ValueTestEntity")
    .addField("localDateTime", LocalDateTime.now())
    .addField("localDate", LocalDate.now())
    .addField("localTime", LocalTime.now())
    .addField("instantTime", Instant.now())
    .addField("booleanValue", true)
    .addField("doubleValue", 1.5430434)
    .execute();

// Create with foreign key relationships
SpeedyResponse response = client.create("Product")
    .addField("name", "client-product-1")
    .addField("description", "test description")
    .addField("category.id", "1")  // Foreign Key to Category entity
    .execute();
```

### Read Operations

```java
// Get entity by ID
SpeedyResponse response = client.get("Category")
    .key("id", "category-id")
    .execute();

// Get entity by ID (alternative syntax)
SpeedyResponse response = client.get("ValueTestEntity")
    .key("id", entityId)
    .execute();
```

### Update Operations

```java
// Update entity by ID
SpeedyResponse response = client.update("Category")
    .key("id", categoryId)
    .field("name", "cat-CLIENT-updated-1")
    .execute();

// Update multiple fields
SpeedyResponse response = client.update("ValueTestEntity")
    .key("id", entityId)
    .field("booleanValue", false)
    .field("localDateTime", LocalDateTime.now().plusDays(1).toString())
    .execute();

// Update product
SpeedyResponse response = client.update("Product")
    .key("id", productId)
    .field("name", "updated-client-product")
    .execute();
```

### Delete Operations

```java
// Delete entity by ID
SpeedyResponse response = client.delete("Category")
    .key("id", categoryId)
    .execute();

// Delete entity by ID (alternative syntax)
SpeedyResponse response = client.delete("ValueTestEntity")
    .key("id", entityId)
    .execute();
```

## Query Operations

### Basic Query

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

SpeedyQuery query = SpeedyQuery.from("Category")
    .where(condition("name", eq("cat-CLIENT-updated-1")))
    .build();

SpeedyResponse response = client.query(query).execute();
```

### Complex Query with Ordering

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

// Query with ordering
SpeedyQuery query = SpeedyQuery.from("ValueTestEntity")
    .orderByAsc("localDate")
    .build();

SpeedyResponse response = client.query(query).execute();

// Query with descending order
SpeedyQuery query = SpeedyQuery.from("ValueTestEntity")
    .orderByDesc("localTime")
    .build();

SpeedyResponse response = client.query(query).execute();
```

### Date and Time Queries

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

// Query by date range
SpeedyQuery query = SpeedyQuery.from("ValueTestEntity")
    .where(condition("localDate", gt(LocalDate.now().toString())))
    .build();

SpeedyResponse response = client.query(query).execute();

// Query by time
SpeedyQuery query = SpeedyQuery.from("ValueTestEntity")
    .where(condition("localTime", gt(LocalTime.of(11, 0).toString())))
    .build();

SpeedyResponse response = client.query(query).execute();

// Query by instant
SpeedyQuery query = SpeedyQuery.from("ValueTestEntity")
    .where(condition("instantTime", lt(Instant.now().toString())))
    .build();

SpeedyResponse response = client.query(query).execute();
```

### Numeric Queries

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

// Query by double value
SpeedyQuery query = SpeedyQuery.from("ValueTestEntity")
    .where(condition("doubleValue", eq(1.5430434)))
    .build();

SpeedyResponse response = client.query(query).execute();
```

## Testing Examples

### Unit Testing with MockMvc

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCreateUser() {
        SpeedyClient<ResultActions> client = SpeedyClient.mockMvc(mockMvc);
        
        ResultActions result = client.create("users")
            .addField("name", "John Doe")
            .addField("email", "john@example.com")
            .execute();
            
        result.andExpect(status().isCreated())
              .andExpect(jsonPath("$.name").value("John Doe"));
    }
    
    @Test
    void testGetUsers() {
        SpeedyClient<ResultActions> client = SpeedyClient.mockMvc(mockMvc);
        
        ResultActions result = client.get("users")
            .select("id", "name")
            .execute();
            
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    void testQueryUsers() {
        SpeedyClient<ResultActions> client = SpeedyClient.mockMvc(mockMvc);
        
        SpeedyQuery query = from("users")
            .where(condition("active", eq(true)))
            .select("id", "name")
            .build();
        
        ResultActions result = client.query(query).execute();
        
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$").isArray());
    }
}
```

## Response Handling

### Basic Response Handling

```java
SpeedyResponse response = client.get("users").execute();

if (response.isSuccess()) {
    List<User> users = response.getData();
    System.out.println("Found " + users.size() + " users");
} else {
    System.err.println("Error: " + response.getError());
}
```

### Typed Response Handling

```java
// For custom response types
CustomResponse response = client.get("users").execute();

if (response.getStatus() == 200) {
    List<User> users = response.getUsers();
    // Process users
} else {
    // Handle error
    String error = response.getErrorMessage();
}
```

## Error Handling

### Exception Handling

```java
try {
    SpeedyResponse response = client.create("users")
        .addField("name", "John Doe")
        .addField("email", "john@example.com")
        .execute();
        
    if (!response.isSuccess()) {
        log.error("API Error: {}", response.getError());
    }
} catch (SpeedyClientException e) {
    log.error("Client error: {}", e.getMessage());
} catch (Exception e) {
    log.error("Unexpected error: {}", e.getMessage());
}
```

### Validation Errors

```java
SpeedyResponse response = client.create("users")
    .addField("name", "")  // Invalid empty name
    .addField("email", "invalid-email")  // Invalid email format
    .execute();

if (!response.isSuccess()) {
    ValidationError error = response.getValidationError();
    if (error != null) {
        error.getFieldErrors().forEach(fieldError -> {
            log.error("Field {}: {}", fieldError.getField(), fieldError.getMessage());
        });
    }
}
```

## Best Practices

### 1. Use Static Imports for Queries

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

// Much cleaner than SpeedyQuery.from("users")
SpeedyQuery query = from("users")
    .where(condition("active", eq(true)))
    .build();
```

### 2. Handle Responses Properly

```java
SpeedyResponse response = client.get("users").execute();
if (response.isSuccess()) {
    // Process data
} else {
    // Handle error appropriately
}
```

### 3. Use Appropriate HTTP Clients

- **Production**: Use `RestTemplate` with proper configuration
- **Testing**: Use `MockMvc` for fast, reliable tests
- **Custom**: Implement `HttpClient<T>` for special requirements

### 4. Optimize Queries

```java
// Good: Select only needed fields
SpeedyQuery query = from("users")
    .select("id", "name", "email")  // Only get what you need
    .where(condition("active", eq(true)))
    .pageSize(20)  // Reasonable page size
    .build();

// Avoid: Getting all fields without pagination
SpeedyQuery badQuery = from("users").build();  // Gets everything!
```

### 5. Reuse Client Instances

```java
// Good: Reuse client instance
@Service
public class UserService {
    private final SpeedyClient<SpeedyResponse> client;
    
    public UserService(RestTemplate restTemplate) {
        this.client = SpeedyClient.restTemplate(restTemplate, "https://api.example.com");
    }
    
    public List<User> getActiveUsers() {
        SpeedyQuery query = from("users")
            .where(condition("active", eq(true)))
            .build();
        return client.query(query).execute().getData();
    }
}
```

## Factory Methods Reference

| Method | Description | Use Case |
|--------|-------------|----------|
| `restTemplate(RestTemplate, String)` | Creates client with RestTemplate | Production applications |
| `mockMvc(MockMvc)` | Creates client with MockMvc | Unit/integration testing |
| `from(HttpClient<T>)` | Creates client with custom HTTP client | Custom implementations |

## Available Operations

| Operation | Method | Description | Returns |
|-----------|--------|-------------|---------|
| Create | `create(String entityName)` | Create new entities | `SpeedyCreateRequestBuilder<T>` |
| Read | `get(String entityName)` | Retrieve entities | `SpeedyGetRequestBuilder<T>` |
| Update | `update(String entityName)` | Update existing entities | `SpeedyUpdateRequestBuilder<T>` |
| Delete | `delete(String entityName)` | Delete entities | `SpeedyDeleteRequestBuilder<T>` |
| Query | `query(SpeedyQuery query)` | Execute custom queries | `SpeedyQueryRequest<T>` |