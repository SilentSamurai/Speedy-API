package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.policy.condition.PolicyConditionFactory;
import com.github.silent.samurai.speedy.policy.condition.QueryConditionFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/// Resolves a condition {@code "type"} discriminator to the {@link PolicyConditionFactory} that
/// parses it. Populated with Speedy's built-ins, every {@link PolicyConditionFactory} found via
/// {@code ServiceLoader}, and any factories the application registers explicitly — adding a new
/// condition type never requires touching this class (requirement 18).
public class PolicyConditionRegistry {

    private final Map<String, PolicyConditionFactory> factories = new LinkedHashMap<>();

    public PolicyConditionRegistry() {
        register(new QueryConditionFactory());
        for (PolicyConditionFactory factory : ServiceLoader.load(PolicyConditionFactory.class)) {
            register(factory);
        }
    }

    public final void register(PolicyConditionFactory factory) {
        factories.put(factory.type(), factory);
    }

    public Optional<PolicyConditionFactory> get(String type) {
        return Optional.ofNullable(factories.get(type));
    }
}
