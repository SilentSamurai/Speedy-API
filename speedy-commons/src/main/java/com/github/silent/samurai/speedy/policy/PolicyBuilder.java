package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.policy.condition.QueryCondition;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import com.github.silent.samurai.speedy.utils.Speedy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Fluent builder for the request-scoped {@link SpeedyAuthContext} returned by
/// {@code ISpeedyConfiguration.authContextPerReq()}, and the only way to make one.
///
/// <p>Use {@link #denyByDefault()} for a fail-closed policy document, add the trusted caller
/// variables and rules that apply to the request, then call {@link #build()}. Already hold a
/// document? {@link #from(PolicyDocument)} seeds a builder from it, so a shared, immutable
/// document can be paired with fresh caller variables on every request.</p>
///
/// <p>Variables are supplied as plain Java values and converted here, which is what keeps
/// {@code SpeedyValue} out of application code — see {@link #variable(String, Object)}.</p>
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolicyBuilder {

    private final PolicyEffect defaultEffect;
    private final Map<String, SpeedyValue> variables = new LinkedHashMap<>();
    private final List<SpeedyPolicy> rules = new ArrayList<>();

    /// Starts a policy document whose unmatched actions and resources are denied.
    public static PolicyBuilder denyByDefault() {
        return new PolicyBuilder(PolicyEffect.DENY);
    }

    /// Starts a policy document whose unmatched actions and resources are allowed.
    public static PolicyBuilder allowByDefault() {
        return new PolicyBuilder(PolicyEffect.ALLOW);
    }

    /// Seeds a builder from an existing document, adopting its default effect and rules. Lets a
    /// shared, immutable {@link PolicyDocument} built once at startup be combined with the
    /// variables of whichever caller made the current request.
    public static PolicyBuilder from(PolicyDocument document) {
        PolicyBuilder builder = new PolicyBuilder(document.defaultEffect());
        document.speedyPolicies().forEach(builder::rule);
        return builder;
    }

    /// Adds the standard {@code principal.id} variable used by policy conditions.
    public PolicyBuilder principalId(String principalId) {
        return variable("principal.id", principalId);
    }

    /// Adds a trusted variable that policy conditions can reference as {@code ${name}}.
    ///
    /// <p>{@code value} is a plain Java value — {@code String}, {@code Boolean}, {@code Long},
    /// {@code Integer}, {@code Double}, {@code Float}, {@code LocalDate}, {@code LocalTime},
    /// {@code LocalDateTime}, {@code ZonedDateTime}, or {@code null} — converted here, so callers
    /// never build a {@code SpeedyValue}. An unsupported type is rejected at this call.</p>
    ///
    /// @throws ConversionException if {@code value} is not one of the supported types
    public PolicyBuilder variable(String name, Object value) {
        try {
            variables.put(name, Speedy.from(value));
        } catch (ConversionException e) {
            throw new ConversionException("Policy variable '" + name + "': " + e.getMessage(), e);
        }
        return this;
    }

    /// Adds an unconditional allow rule for one action and subject selector.
    public PolicyBuilder allow(String id, PermissionType action, String subject) {
        return allow(id, action, subject, List.of());
    }

    /// Adds an allow rule for one action and subject selector, subject to all supplied conditions.
    ///
    /// <p>For READ, conditions only gate row visibility, and only take effect when {@code subject}
    /// is whole-entity ({@code "Entity"} / {@code "Entity.*"}); a field-specific {@code subject}
    /// (e.g. {@code "Entity.field"}) makes field-level READ visibility unconditional regardless of
    /// any conditions supplied here. For CREATE/UPDATE/REPLACE, conditions are honored for field-specific
    /// subjects too, since they validate a write rather than gate read visibility.</p>
    public PolicyBuilder allow(String id, PermissionType action, String subject, QueryCondition... conditions) {
        return allow(id, action, subject, List.of(conditions));
    }

    /// Adds an allow rule for one action and subject selector, subject to all supplied conditions.
    ///
    /// <p>See {@link #allow(String, PermissionType, String, QueryCondition...)} for how READ
    /// treats field-specific vs. whole-entity subjects differently.</p>
    public PolicyBuilder allow(String id, PermissionType action, String subject, List<QueryCondition> conditions) {
        return addRule(id, PolicyEffect.ALLOW, Set.of(action), subject, conditions);
    }

    /// Adds an unconditional deny rule for one action and subject selector.
    public PolicyBuilder deny(String id, PermissionType action, String subject) {
        return addRule(id, PolicyEffect.DENY, Set.of(action), subject, List.of());
    }

    /// Adds a previously constructed rule to this document.
    public PolicyBuilder rule(SpeedyPolicy rule) {
        rules.add(rule);
        return this;
    }

    /// Builds the policy document without caller variables.
    public PolicyDocument buildDocument() {
        return new PolicyDocument(defaultEffect, rules);
    }

    /// Builds the request-scoped authorization context.
    public SpeedyAuthContext build() {
        return new SpeedyAuthContext(buildDocument(), Map.copyOf(variables));
    }

    private PolicyBuilder addRule(String id, PolicyEffect effect, Set<PermissionType> actions,
                                  String subject, List<QueryCondition> conditions) {
        rules.add(new SpeedyPolicy(id, effect, actions, subject, conditions));
        return this;
    }
}
