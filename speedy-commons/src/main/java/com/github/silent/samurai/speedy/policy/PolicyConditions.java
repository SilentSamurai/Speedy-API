package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.policy.condition.QueryCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Factory methods for the query-style row conditions used by {@link PolicyBuilder} rules.
public final class PolicyConditions {

    private PolicyConditions() {
    }

    /// Returns a value reference such as {@code ${principal.id}} for use in a condition.
    public static String variable(String name) {
        return "${" + name + "}";
    }

    /// Creates a condition requiring {@code field} to equal {@code value}.
    public static QueryCondition fieldEquals(String field, Object value) {
        return new QueryCondition(Map.of(field, value));
    }

    /// Creates a condition requiring {@code field} to equal one of {@code values}.
    public static QueryCondition fieldIn(String field, Object... values) {
        return new QueryCondition(Map.of(field, Map.of("$in", List.of(values))));
    }

    /// Creates an {@code $or} condition in which {@code field} equals any supplied value.
    public static QueryCondition orFieldEquals(String field, Object... values) {
        List<Map<String, Object>> alternatives = new ArrayList<>(values.length);
        for (Object value : values) {
            alternatives.add(Map.of(field, value));
        }
        return new QueryCondition(Map.of("$or", alternatives));
    }
}
