package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.policy.condition.QueryCondition;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Fluent builder for the request-scoped {@link SpeedyAuthContext} returned by
/// {@code ISpeedyConfiguration.authContextPerReq()}.
///
/// <p>Use {@link #denyByDefault()} for a fail-closed policy document, add the trusted caller
/// variables and rules that apply to the request, then call {@link #build()}.</p>
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

    /// Adds the standard {@code principal.id} variable used by policy conditions.
    public PolicyBuilder principalId(String principalId) {
        return variable("principal.id", new SpeedyText(principalId));
    }

    /// Adds a trusted variable that policy conditions can reference as {@code ${name}}.
    public PolicyBuilder variable(String name, SpeedyValue value) {
        variables.put(name, value);
        return this;
    }

    /// Adds an unconditional allow rule for one action and subject selector.
    public PolicyBuilder allow(String id, PermissionType action, String subject) {
        return allow(id, action, subject, List.of());
    }

    /// Adds an allow rule for one action and subject selector, subject to all supplied conditions.
    public PolicyBuilder allow(String id, PermissionType action, String subject, QueryCondition... conditions) {
        return allow(id, action, subject, List.of(conditions));
    }

    /// Adds an allow rule for one action and subject selector, subject to all supplied conditions.
    public PolicyBuilder allow(String id, PermissionType action, String subject, List<QueryCondition> conditions) {
        return addRule(id, PolicyEffect.ALLOW, Set.of(action), subject, conditions);
    }

    /// Adds an allow rule for multiple actions and one subject selector, subject to all supplied conditions.
    public PolicyBuilder allow(String id, Set<PermissionType> actions, String subject, QueryCondition... conditions) {
        return addRule(id, PolicyEffect.ALLOW, actions, subject, List.of(conditions));
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
        return new SpeedyAuthContext(buildDocument(), variables);
    }

    private PolicyBuilder addRule(String id, PolicyEffect effect, Set<PermissionType> actions,
                                  String subject, List<QueryCondition> conditions) {
        rules.add(new SpeedyPolicy(id, effect, actions, List.of(subject), conditions));
        return this;
    }
}
