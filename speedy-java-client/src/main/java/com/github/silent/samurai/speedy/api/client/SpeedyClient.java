package com.github.silent.samurai.speedy.api.client;

import com.github.silent.samurai.speedy.api.client.builder.SpeedyCreateRequestBuilder;
import com.github.silent.samurai.speedy.api.client.builder.SpeedyDeleteRequestBuilder;
import com.github.silent.samurai.speedy.api.client.builder.SpeedyGetRequestBuilder;
import com.github.silent.samurai.speedy.api.client.builder.SpeedyUpdateRequestBuilder;
import com.github.silent.samurai.speedy.api.client.clients.MockMvcHttpClient;
import com.github.silent.samurai.speedy.api.client.clients.RestTemplateSpeedyClientImpl;
import com.github.silent.samurai.speedy.api.client.models.SpeedyQueryRequest;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestTemplate;

/// # SpeedyClient
/// 
/// A generic client class that provides a fluent API for interacting with the Speedy API.
/// This class serves as the main entry point for making CRUD operations and queries against
/// a Speedy-enabled backend.
/// 
/// ## Key Features
/// 
/// - **Generic Type Support**: Uses type parameter `T` to handle different response types
/// - **Builder Pattern**: Provides builder classes for different HTTP operations
/// - **HTTP Client Abstraction**: Supports different HTTP client implementations
/// - **Query Support**: Allows execution of custom queries using [SpeedyQuery]
/// 
/// ## Usage Examples
/// 
/// ### Creating a client with RestTemplate
/// ```java
/// RestTemplate restTemplate = new RestTemplate();
/// SpeedyClient<SpeedyResponse> client = SpeedyClient.restTemplate(restTemplate, "http://localhost:8080");
/// 
/// // Create a new user
/// SpeedyResponse response = client.create("users")
///     .withBody(userData)
///     .execute();
/// ```
/// 
/// ### Creating a client with MockMvc for testing
/// ```java
/// SpeedyClient<ResultActions> testClient = SpeedyClient.mockMvc(mockMvc);
/// 
/// // Test user creation
/// ResultActions result = testClient.create("users")
///     .withBody(userData)
///     .execute();
/// result.andExpect(status().isCreated());
/// ```
/// 
/// ### Using a custom HTTP client
/// ```java
/// HttpClient<CustomResponse> customClient = new CustomHttpClient();
/// SpeedyClient<CustomResponse> client = SpeedyClient.from(customClient);
/// ```
/// 
/// ## Available Operations
/// 
/// | Operation | Method | Description | Returns |
/// |-----------|--------|-------------|---------|
/// | Create | `create(String entityName)` | Create new entities | [SpeedyCreateRequestBuilder] |
/// | Read | `get(String entityName)` | Retrieve entities | [SpeedyGetRequestBuilder] |
/// | Update | `update(String entityName)` | Update existing entities | [SpeedyUpdateRequestBuilder] |
/// | Delete | `delete(String entityName)` | Delete entities | [SpeedyDeleteRequestBuilder] |
/// | Query | `query(SpeedyQuery query)` | Execute custom queries | [SpeedyQueryRequest] |
/// 
/// ## Factory Methods
/// 
/// | Method | Description | Use Case |
/// |--------|-------------|----------|
/// `restTemplate(RestTemplate, String)` | Creates client with RestTemplate | Production applications |
/// `mockMvc(MockMvc)` | Creates client with MockMvc | Unit/integration testing |
/// `from(HttpClient<T>)` | Creates client with custom HTTP client | Custom implementations |
/// 
/// @param <T> the response type for this client
public class SpeedyClient<T> {

    private final SpeedyApi<T> speedyApi;

    /// Creates a new SpeedyClient with the specified HTTP client implementation.
    /// 
    /// @param httpClient the HTTP client to use for API requests
    public SpeedyClient(HttpClient<T> httpClient) {
        this.speedyApi = new SpeedyApi<>(httpClient);
    }

    /// Creates a SpeedyClient using Spring's RestTemplate for HTTP requests.
    ///
    /// This factory method is ideal for production applications that need to make
    /// HTTP requests to a Speedy API server.
    ///
    /// ## Example
    /// ```java
    /// RestTemplate = new RestTemplate();
    /// SpeedyClient<SpeedyResponse> client = SpeedyClient.restTemplate(restTemplate, "https://api.example.com");
    ///
    /// // Use the client
    /// SpeedyResponse response = client.get("users").execute();
    /// ```
    ///
    /// @param restTemplate the RestTemplate instance to use for HTTP requests
    /// @param baseUrl the base URL of the Speedy API server
    /// @return a SpeedyClient configured with RestTemplate
    /// @see [RestTemplateSpeedyClientImpl]
    public static SpeedyClient<SpeedyResponse> restTemplate(RestTemplate restTemplate, String baseUrl) {
        return new SpeedyClient<>(new RestTemplateSpeedyClientImpl(restTemplate, baseUrl));
    }

    /// Creates a SpeedyClient using MockMvc for testing purposes.
    /// 
    /// This factory method is particularly useful for:
    /// - **Unit testing** API endpoints
    /// - **Integration testing** without starting a full web server
    /// - **Performance testing** with fast, in-memory requests
    /// 
    /// ## Example
    /// ```java
    /// @Test
    /// public void testUserCreation() {
    ///     SpeedyClient<ResultActions> client = SpeedyClient.mockMvc(mockMvc);
    ///     ResultActions result = client.create("users")
    ///         .withBody(userData)
    ///         .execute();
    ///     result.andExpect(status().isCreated());
    /// }
    /// ```
    /// 
    /// @param mockMvc the MockMvc instance to use for HTTP requests
    /// @return a SpeedyClient configured with MockMvc
    /// @see [MockMvcHttpClient]
    public static SpeedyClient<ResultActions> mockMvc(MockMvc mockMvc) {
        return new SpeedyClient<>(new MockMvcHttpClient(mockMvc));
    }

    /// Creates a SpeedyClient from a custom HTTP client implementation.
    /// 
    /// This factory method allows you to use any custom HTTP client that implements
    /// the [HttpClient] interface.
    /// 
    /// ## Example
    /// ```java
    /// HttpClient<CustomResponse> customClient = new CustomHttpClient();
    /// SpeedyClient<CustomResponse> client = SpeedyClient.from(customClient);
    /// ```
    /// 
    /// @param <T> the response type for the custom client
    /// @param httpClient the custom HTTP client implementation
    /// @return a SpeedyClient configured with the custom HTTP client
    public static <T> SpeedyClient<T> from(HttpClient<T> httpClient) {
        return new SpeedyClient<>(httpClient);
    }

    /// Creates a builder for updating existing entities.
    /// 
    /// ## Example
    /// ```java
    /// SpeedyResponse response = client.update("users")
    ///     .withId(123)
    ///     .withBody(updatedUserData)
    ///     .execute();
    /// ```
    /// 
    /// @param entityName the name of the entity to update
    /// @return a builder for constructing update requests
    /// @throws IllegalArgumentException if entityName is null or empty
    public SpeedyUpdateRequestBuilder<T> update(String entityName) {
        return new SpeedyUpdateRequestBuilder<>(entityName, speedyApi);
    }

    /// Creates a builder for retrieving entities.
    /// 
    /// ## Example
    /// ```java
    /// // Get all users
    /// SpeedyResponse response = client.get("users").execute();
    /// 
    /// // Get user by ID
    /// SpeedyResponse user = client.get("users")
    ///     .withId(123)
    ///     .execute();
    /// ```
    /// 
    /// @param entityName the name of the entity to retrieve
    /// @return a builder for constructing get requests
    /// @throws IllegalArgumentException if entityName is null or empty
    public SpeedyGetRequestBuilder<T> get(String entityName) {
        return new SpeedyGetRequestBuilder<>(entityName, speedyApi);
    }

    /// Creates a builder for deleting entities.
    /// 
    /// ## Example
    /// ```java
    /// SpeedyResponse response = client.delete("users")
    ///     .withId(123)
    ///     .execute();
    /// ```
    /// 
    /// @param entityName the name of the entity to delete
    /// @return a builder for constructing delete requests
    /// @throws IllegalArgumentException if entityName is null or empty
    public SpeedyDeleteRequestBuilder<T> delete(String entityName) {
        return new SpeedyDeleteRequestBuilder<>(entityName, speedyApi);
    }

    /// Creates a builder for creating new entities.
    /// 
    /// ## Example
    /// ```java
    /// SpeedyResponse response = client.create("users")
    ///     .withBody(newUserData)
    ///     .execute();
    /// ```
    /// 
    /// @param entityName the name of the entity to create
    /// @return a builder for constructing create requests
    /// @throws IllegalArgumentException if entityName is null or empty
    public SpeedyCreateRequestBuilder<T> create(String entityName) {
        return new SpeedyCreateRequestBuilder<>(entityName, speedyApi);
    }

    /// Creates a query request for executing custom queries.
    /// 
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("active", eq(true)))
    ///     .build();
    /// 
    /// SpeedyQueryRequest<T> request = client.query(query);
    /// SpeedyResponse response = request.execute();
    /// ```
    /// 
    /// @param query the custom query to execute
    /// @return a query request for executing the specified query
    /// @throws IllegalArgumentException if query is null
    public SpeedyQueryRequest<T> query(SpeedyQuery query) {
        return new SpeedyQueryRequest<>(query, speedyApi);
    }

}
