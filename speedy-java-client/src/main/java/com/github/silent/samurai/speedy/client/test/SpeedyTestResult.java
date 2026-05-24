package com.github.silent.samurai.speedy.client.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test-oriented result that combines typed data access with HTTP status
 * and JSONPath assertions for MockMvc-based integration tests.
 *
 * <p>Unlike {@link com.github.silent.samurai.speedy.client.SpeedyResult}, this class does NOT
 * throw exceptions on 4xx/5xx — assertions are explicit.
 */
public class SpeedyTestResult {

    private final ResultActions resultActions;
    private final JsonNode payload;
    private final ObjectMapper mapper;

    public SpeedyTestResult(ResultActions resultActions, JsonNode payload,
                            ObjectMapper mapper) {
        this.resultActions = resultActions;
        this.payload = payload != null ? payload : mapper.createArrayNode();
        this.mapper = mapper;
    }

    /**
     * Asserts the response HTTP status code.
     */
    public SpeedyTestResult expectStatus(int expected) {
        try {
            resultActions.andExpect(status().is(expected));
        } catch (Exception e) {
            throw new AssertionError("Expected status " + expected + ": " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Asserts HTTP 200 OK.
     */
    public SpeedyTestResult expectOk() {
        return expectStatus(200);
    }

    /**
     * Asserts HTTP 201 Created.
     */
    public SpeedyTestResult expectCreated() {
        return expectStatus(201);
    }

    /**
     * Asserts HTTP 400 Bad Request.
     */
    public SpeedyTestResult expectBadRequest() {
        return expectStatus(400);
    }

    /**
     * Asserts HTTP 404 Not Found.
     */
    public SpeedyTestResult expectNotFound() {
        return expectStatus(404);
    }

    /**
     * Asserts a JSONPath expression using a Hamcrest matcher.
     */
    public SpeedyTestResult expectJsonPath(String expression, org.hamcrest.Matcher<?> matcher) {
        try {
            resultActions.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(expression, matcher));
        } catch (Exception e) {
            throw new AssertionError("JSONPath assertion failed for '" + expression + "': " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Asserts a JSONPath expression equals the expected value.
     */
    public SpeedyTestResult expectJsonPath(String expression, Object expected) {
        try {
            resultActions.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(expression).value(expected));
        } catch (Exception e) {
            throw new AssertionError("JSONPath assertion failed for '" + expression + "': " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Asserts a JSONPath expression exists (value is present and not null).
     */
    public SpeedyTestResult expectJsonPathExists(String expression) {
        try {
            resultActions.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(expression).exists());
        } catch (Exception e) {
            throw new AssertionError("JSONPath existence assertion failed for '" + expression + "': " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Deserializes all entities as a typed list.
     */
    public <T> List<T> list(Class<T> type) {
        try {
            return mapper.readValue(
                    mapper.treeAsTokens(payload),
                    mapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize response payload", e);
        }
    }

    /**
     * Deserializes the first entity, or null if empty.
     */
    public <T> T first(Class<T> type) {
        if (payload.isEmpty()) {
            return null;
        }
        try {
            return mapper.treeToValue(payload.get(0), type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize first entity", e);
        }
    }

    /**
     * Extracts a value at the given JSONPath expression.
     */
    public <T> T jsonPath(String expression, Class<T> type) {
        try {
            return com.jayway.jsonpath.JsonPath.read(
                    resultActions.andReturn().getResponse().getContentAsString(), expression);
        } catch (Exception e) {
            throw new IllegalStateException("JSONPath extraction failed for '" + expression + "': " + e.getMessage(), e);
        }
    }

    /**
     * Extracts a raw String at the given JSONPath expression.
     */
    public String jsonPath(String expression) {
        return jsonPath(expression, String.class);
    }

    /**
     * Returns the raw response body as a String.
     */
    public String responseBody() {
        try {
            return resultActions.andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get response body", e);
        }
    }

    /**
     * Escape hatch — returns the raw Spring MockMvc {@link ResultActions}
     * for advanced assertions not covered by the built-in methods.
     */
    public ResultActions resultActions() {
        return resultActions;
    }
}
