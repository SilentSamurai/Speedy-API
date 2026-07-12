package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.interfaces.query.Condition;

import java.util.Optional;

/// Extension point for data-dependent (ABAC) policy conditions (requirement: new condition types
/// can be added without redesigning the policy format or touching the decision engine). Each
/// implementation is a plain, self-contained class — no central type-switch anywhere evaluates
/// conditions; a {@link PolicyConditionFactory} on the classpath is all that's needed to parse one
/// out of a policy document.
public interface PolicyCondition {

    /// The discriminator matching this condition's {@code "type"} in the policy JSON.
    String type();

    /// Whether this condition holds for the given row/principal. Implementations must be
    /// fail-closed when {@link ConditionContext#hasRow()} is false (no row means the condition
    /// cannot be verified, so it does not grant).
    boolean isSatisfied(ConditionContext ctx);

    /// Translates this condition into an equivalent query {@link Condition} for WHERE-injection
    /// (row-level read visibility), when such a translation exists. Returns empty when the
    /// condition cannot be faithfully expressed as a query predicate (e.g. it depends on
    /// unavailable principal data) — callers must treat an empty result as "cannot translate",
    /// never as "always true".
    default Optional<Condition> toQueryCondition(ConditionContext ctx) {
        return Optional.empty();
    }
}
