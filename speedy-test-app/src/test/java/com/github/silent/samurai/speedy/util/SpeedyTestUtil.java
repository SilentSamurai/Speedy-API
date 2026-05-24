package com.github.silent.samurai.speedy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

public class SpeedyTestUtil {

    public static DocumentContext jsonPath(String responseBody) throws JsonProcessingException {
        return JsonPath.parse(responseBody);
    }

    /**
     * Entry point for fluent JSONPath assertions using Hamcrest on a response body string.
     */
    public static JsonResponseAssert assertThat(String responseBody) throws JsonProcessingException {
        return new JsonResponseAssert(jsonPath(responseBody));
    }

    /**
     * Fluent JSON response assertion helper that lets you select a JSONPath
     * and then assert on its value using Hamcrest, with support for chaining.
     */
    public static final class JsonResponseAssert {
        private final DocumentContext ctx;

        private JsonResponseAssert(DocumentContext ctx) {
            this.ctx = ctx;
        }

        /**
         * Select a JSONPath and coerce it to the provided type.
         */
        public <T> ValueAssert<T> path(String jsonPath, Class<T> type) {
            T value = ctx.read(jsonPath, type);
            return new ValueAssert<>(this, value, jsonPath);
        }
    }

    /**
     * Holds a selected JSONPath value and allows asserting with Hamcrest.
     * Calling is(...) performs the assertion and returns the parent to allow chaining further paths.
     */
    public static final class ValueAssert<T> {
        private final JsonResponseAssert parent;
        private final T value;
        private final String jsonPath;

        private ValueAssert(JsonResponseAssert parent, T value, String jsonPath) {
            this.parent = parent;
            this.value = value;
            this.jsonPath = jsonPath;
        }

        /**
         * Assert the selected value with the provided Hamcrest matcher and return the parent for chaining.
         */
        @SuppressWarnings({"rawtypes", "unchecked"})
        public JsonResponseAssert is(Matcher<?> matcher) {
            // Safe because Hamcrest performs runtime checks and we intentionally relax compile-time checks here.
            MatcherAssert.assertThat("JSON path: " + jsonPath, value, (Matcher) matcher);
            return parent;
        }

        /**
         * Direct access to the selected value if needed by the test.
         */
        public T get() {
            return value;
        }
    }
}
