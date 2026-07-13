package com.github.silent.samurai.speedy.policy.model;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.policy.condition.QueryCondition;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// A single policy statement for one {@code subject}. Its {@code effect} applies to every matching
/// {@code action}, provided all {@code conditions} are satisfied (AND-combined; an empty list is
/// unconditional).
public record SpeedyPolicy(String id,
                           String role,
                           PolicyEffect effect,
                           Set<PermissionType> action,
                           String subject,
                           List<QueryCondition> conditions) {

    public SpeedyPolicy(String id, String role, PolicyEffect effect, Set<PermissionType> action, String subject, List<QueryCondition> conditions) {
        this.id = id;
        this.role = role;
        this.effect = effect;
        this.action = action == null ? Set.of() : Set.copyOf(action);
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    /// Convenience constructor for policies resolved for the caller before constructing the
    /// document. The optional {@code role} remains available on the full constructor for policy
    /// stores that keep role metadata alongside the rule.
    public SpeedyPolicy(String id, PolicyEffect effect, Set<PermissionType> action, String subject, List<QueryCondition> conditions) {
        this(id, null, effect, action, subject, conditions);
    }

    public String getName() {
        return id;
    }

    public Set<PermissionType> getActions() {
        return action;
    }

    /// Raw {@code "Entity"} / {@code "Entity.field"} / {@code "Entity.*"} selector token,
    /// matched against a request's (entity, field) by {@link com.github.silent.samurai.speedy.policy.PolicyEngine}.
    public String getResource() {
        return subject;
    }

}
