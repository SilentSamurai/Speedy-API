package com.github.silent.samurai.speedy.policy.model;

import java.util.List;

/// The full policy for one request: an ordered list of {@link SpeedyPolicy}s plus the
/// {@code defaultEffect} that applies when no rule matches a given (action, resource) pair.
public record PolicyDocument(PolicyEffect defaultEffect, List<SpeedyPolicy> speedyPolicies) {

    public PolicyDocument(PolicyEffect defaultEffect, List<SpeedyPolicy> speedyPolicies) {
        this.defaultEffect = defaultEffect == null ? PolicyEffect.DENY : defaultEffect;
        this.speedyPolicies = speedyPolicies == null ? List.of() : List.copyOf(speedyPolicies);
    }

}
