package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;

import java.util.Collections;
import java.util.Map;

/// The bundle {@code ISpeedyConfiguration.authContextPerReq()} returns for the current request:
/// the policy document and pre-resolved variables available as {@code ${name}} references in
/// query and policy conditions. Variables like {@code principal.id}, {@code principal.role},
/// etc. should be pre-populated by the configuration implementation.
public record SpeedyAuthContext(PolicyDocument document, Map<String, SpeedyValue> variables) {

    public SpeedyAuthContext(PolicyDocument document) {
        this(document, Collections.emptyMap());
    }
}
