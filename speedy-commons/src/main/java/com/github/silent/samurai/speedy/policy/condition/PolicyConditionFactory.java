package com.github.silent.samurai.speedy.policy.condition;

/// Extension point for parsing a new {@link PolicyCondition} type out of a policy document
/// (requirement: adding a condition type must not require redesigning the policy format or
/// touching the decision engine). Discovered via {@code ServiceLoader} plus any factories an
/// application registers explicitly — see {@code PolicyConditionRegistry} in speedy-core.
public interface PolicyConditionFactory {

    /// Must match the {@code "type"} discriminator this factory parses.
    String type();

    PolicyCondition parse(ConditionSpec spec);
}
