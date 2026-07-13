package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.policy.condition.QueryCondition;

import java.util.Map;

/// Factory methods for the query-style row conditions used by {@link PolicyBuilder} rules.
///
/// <p>These are thin conveniences over {@link QueryCondition}, which accepts the full
/// {@code $or}/{@code $and}/{@code $in}/operator/{@code ${variable}} map grammar directly — reach
/// for {@code new QueryCondition(Map.of(...))} for anything beyond a single field equality.</p>
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
}
