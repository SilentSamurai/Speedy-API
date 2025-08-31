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

/**
 * SpeedyClient is a generic, fluent client for interacting with Speedy API.
 * It provides builder-based CRUD operations and query execution over an
 * underlying HTTP client implementation.
 *
 * <p><b>Key features</b>:</p>
 * <ul>
 *   <li>Generic type support via type parameter {@code T}</li>
 *   <li>Builder pattern for Create/Read/Update/Delete</li>
 *   <li>HTTP client abstraction (RestTemplate, MockMvc, or custom)</li>
 *   <li>Query execution using {@link SpeedyQuery}</li>
 * </ul>
 *
 * <p><b>Usage examples</b>:</p>
 * <pre>{@code
 * // RestTemplate-based client
 * RestTemplate restTemplate = new RestTemplate();
 * SpeedyClient<SpeedyResponse> client = SpeedyClient.restTemplate(restTemplate, "http://localhost:8080");
 *
 * // Create a new user
 * SpeedyResponse response = client.create("users")
 *     .addField("name", "Alice")
 *     .addField("email", "alice@example.com")
 *     .execute();
 * }
 * </pre>
 *
 * <pre>{@code
 * // MockMvc-based client for tests
 * SpeedyClient<ResultActions> testClient = SpeedyClient.mockMvc(mockMvc);
 * ResultActions result = testClient.create("users")
 *     .addField("name", "Bob")
 *     .execute();
 * // e.g. result.andExpect(status().isCreated());
 * }
 * </pre>
 *
 * <pre>{@code
 * // Using a custom HttpClient
 * HttpClient<CustomResponse> customClient = new CustomHttpClient();
 * SpeedyClient<CustomResponse> custom = SpeedyClient.from(customClient);
 * }
 * </pre>
 *
 * <p><b>Factory methods</b>:</p>
 * <ul>
 *   <li>{@link #restTemplate(RestTemplate, String)} – production usage</li>
 *   <li>{@link #mockMvc(MockMvc)} – unit/integration tests</li>
 *   <li>{@link #from(HttpClient)} – custom implementations</li>
 * </ul>
 *
 * @param <T> the response type for this client
 */
 public class SpeedyClient<T> {

    private final SpeedyApi<T> speedyApi;

    /**
     * Creates a new SpeedyClient with the specified HTTP client implementation.
     *
     * @param httpClient the HTTP client to use for API requests
     */
    public SpeedyClient(HttpClient<T> httpClient) {
        this.speedyApi = new SpeedyApi<>(httpClient);
    }

    /**
     * Creates a SpeedyClient using Spring's RestTemplate for HTTP requests.
     * Ideal for production applications that call a Speedy API server.
     *
     * <pre>{@code
     * RestTemplate restTemplate = new RestTemplate();
     * SpeedyClient<SpeedyResponse> client = SpeedyClient.restTemplate(restTemplate, "https://api.example.com");
     * SpeedyResponse response = client.get("users").execute();
     * }
     * </pre>
     *
     * @param restTemplate the RestTemplate instance to use for HTTP requests
     * @param baseUrl      the base URL of the Speedy API server
     * @return a SpeedyClient configured with RestTemplate
     * @see RestTemplateSpeedyClientImpl
     */
    public static SpeedyClient<SpeedyResponse> restTemplate(RestTemplate restTemplate, String baseUrl) {
        return new SpeedyClient<>(new RestTemplateSpeedyClientImpl(restTemplate, baseUrl));
    }

    /**
     * Creates a SpeedyClient using MockMvc for testing purposes.
     * Useful for unit/integration tests without starting a web server.
     *
     * <pre>{@code
     * @Test
     * public void testUserCreation() throws Exception {
     *     SpeedyClient<ResultActions> client = SpeedyClient.mockMvc(mockMvc);
     *     ResultActions result = client.create("users")
     *         .addField("name", "Test")
     *         .execute();
     *     // result.andExpect(status().isCreated());
     * }
     * }
     * </pre>
     *
     * @param mockMvc the MockMvc instance to use for HTTP requests
     * @return a SpeedyClient configured with MockMvc
     * @see MockMvcHttpClient
     */
    public static SpeedyClient<ResultActions> mockMvc(MockMvc mockMvc) {
        return new SpeedyClient<>(new MockMvcHttpClient(mockMvc));
    }

    /**
     * Creates a SpeedyClient from a custom HTTP client implementation.
     *
     * <pre>{@code
     * HttpClient<CustomResponse> customClient = new CustomHttpClient();
     * SpeedyClient<CustomResponse> client = SpeedyClient.from(customClient);
     * }
     * </pre>
     *
     * @param <T>        the response type for the custom client
     * @param httpClient the custom HTTP client implementation
     * @return a SpeedyClient configured with the custom HTTP client
     */
    public static <T> SpeedyClient<T> from(HttpClient<T> httpClient) {
        return new SpeedyClient<>(httpClient);
    }

    /**
     * Creates a builder for updating existing entities.
     *
     * <pre>{@code
     * SpeedyResponse response = client.update("users")
     *     .key("id", 123)
     *     .field("name", "New Name")
     *     .execute();
     * }
     * </pre>
     *
     * @param entityName the name of the entity to update
     * @return a builder for constructing update requests
     */
    public SpeedyUpdateRequestBuilder<T> update(String entityName) {
        return new SpeedyUpdateRequestBuilder<>(entityName, speedyApi);
    }

    /**
     * Creates a builder for retrieving entities.
     *
     * <pre>{@code
     * // Get all users
     * SpeedyResponse all = client.get("users").execute();
     *
     * // Get user by ID
     * SpeedyResponse user = client.get("users")
     *     .key("id", 123)
     *     .execute();
     * }
     * </pre>
     *
     * @param entityName the name of the entity to retrieve
     * @return a builder for constructing get requests
     */
    public SpeedyGetRequestBuilder<T> get(String entityName) {
        return new SpeedyGetRequestBuilder<>(entityName, speedyApi);
    }

    /**
     * Creates a builder for deleting entities.
     *
     * <pre>{@code
     * SpeedyResponse response = client.delete("users")
     *     .key("id", 123)
     *     .execute();
     * }
     * </pre>
     *
     * @param entityName the name of the entity to delete
     * @return a builder for constructing delete requests
     */
    public SpeedyDeleteRequestBuilder<T> delete(String entityName) {
        return new SpeedyDeleteRequestBuilder<>(entityName, speedyApi);
    }

    /**
     * Creates a builder for creating new entities.
     *
     * <pre>{@code
     * SpeedyResponse response = client.create("users")
     *     .addField("name", "Alice")
     *     .addField("email", "alice@example.com")
     *     .execute();
     * }
     * </pre>
     *
     * @param entityName the name of the entity to create
     * @return a builder for constructing create requests
     */
    public SpeedyCreateRequestBuilder<T> create(String entityName) {
        return new SpeedyCreateRequestBuilder<>(entityName, speedyApi);
    }

    /**
     * Creates a query request for executing custom queries.
     *
     * <pre>{@code
     * import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
     * SpeedyQuery query = from("users").where(condition("active", eq(true))).build();
     * SpeedyQueryRequest<T> request = client.query(query);
     * SpeedyResponse response = request.execute();
     * }
     * </pre>
     *
     * @param query the custom query to execute
     * @return a query request for executing the specified query
     */
    public SpeedyQueryRequest<T> query(SpeedyQuery query) {
        return new SpeedyQueryRequest<>(query, speedyApi);
    }

}
