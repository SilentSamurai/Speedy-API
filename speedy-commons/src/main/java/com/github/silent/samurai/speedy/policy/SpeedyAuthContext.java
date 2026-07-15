package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;

import java.util.Map;

/// The bundle {@code ISpeedyConfiguration.authContextPerReq()} returns for the current request:
/// the caller's policy document plus the pre-resolved variables its conditions reference as
/// {@code ${name}} — {@code principal.id}, {@code principal.role}, and so on.
///
/// <p>An opaque handle. Build one with {@link PolicyBuilder}, return it from
/// {@code authContextPerReq()}, and let Speedy read it; there is nothing here for an application
/// to call. Its contents are deliberately off the public API, which is what keeps
/// {@code SpeedyValue} an internal representation: variables go in as plain Java values through
/// {@link PolicyBuilder#variable(String, Object)}, and the conversion happens there.</p>
public final class SpeedyAuthContext {

    private final PolicyDocument document;
    private final Map<String, SpeedyValue> variables;

    /// Package-private on purpose: {@link PolicyBuilder} is the only way to make one, so an
    /// application can never be handed the job of constructing {@link SpeedyValue}s itself.
    SpeedyAuthContext(PolicyDocument document, Map<String, SpeedyValue> variables) {
        this.document = document;
        this.variables = variables;
    }

    PolicyDocument document() {
        return document;
    }

    Map<String, SpeedyValue> variables() {
        return variables;
    }
}
