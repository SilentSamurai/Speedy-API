package com.github.silent.samurai.speedy.policy.model;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.policy.condition.PolicyCondition;

import java.util.List;
import java.util.Set;

/// A single policy statement: {@code effect} applies to every ({@code action}, {@code subject})
/// pair it matches, provided all {@code conditions} are satisfied (AND-combined; an empty list is
/// unconditional).
public record SpeedyPolicy(String id,
                           String role,
                           PolicyEffect effect,
                           Set<PermissionType> action,
                           List<ResourceSelector> subject,
                           List<PolicyCondition> conditions) {

    public SpeedyPolicy(String id, String role, PolicyEffect effect, Set<PermissionType> action, List<ResourceSelector> subject, List<PolicyCondition> conditions) {
        this.id = id;
        this.role = role;
        this.effect = effect;
        this.action = action == null ? Set.of() : Set.copyOf(action);
        this.subject = subject == null ? List.of() : List.copyOf(subject);
        this.conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    /// Convenience constructor for policies resolved for the caller before constructing the
    /// document. The optional {@code role} remains available on the full constructor for policy
    /// stores that keep role metadata alongside the rule.
    public SpeedyPolicy(String id, PolicyEffect effect, Set<PermissionType> action, List<ResourceSelector> subject, List<PolicyCondition> conditions) {
        this(id, null, effect, action, subject, conditions);
    }

    public String getName() {
        return id;
    }

    public Set<PermissionType> getActions() {
        return action;
    }

    public List<ResourceSelector> getResources() {
        return subject;
    }

}
