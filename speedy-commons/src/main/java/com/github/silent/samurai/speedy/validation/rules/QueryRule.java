package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * A single, composable validation unit for a query {@code WHERE} condition.
 * Implementations should be <b>stateless</b> and thread-safe.
 * <p>
 * This is the read-side counterpart to {@link FieldRule}: where {@code FieldRule}
 * validates a concrete field <i>value</i> on a write payload, a {@code QueryRule}
 * validates a {@code field + operator + literal} triple of a filter condition.
 * <p>
 * Rules live here (backend-agnostic core) rather than in a query-processor
 * implementation so that the same operator/type constraints apply regardless of
 * which {@code QueryProcessor} executes the query.
 */
public interface QueryRule {

    /**
     * Validate a single binary condition.
     *
     * @param field    metadata of the field on the left-hand side of the condition
     *                 (already resolved for FK traversals)
     * @param operator the condition operator (EQ, LT, PATTERN_MATCHING, …)
     * @param literal  the right-hand side literal value, or {@code null} when the
     *                 right-hand side is not a literal (e.g. a {@code $field} reference)
     * @param errors   mutable list to add error messages – DO NOT throw; accumulate
     */
    void validate(FieldMetadata field, ConditionOperator operator, SpeedyValue literal, List<String> errors);
}
