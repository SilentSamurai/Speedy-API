package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PrincipalVariableResolver {

    private PrincipalVariableResolver() {
    }

    /// Copies all {@code principal.*} entries from the source variables into a new map.
    public static Map<String, SpeedyValue> resolve(Map<String, SpeedyValue> variables) {
        Map<String, SpeedyValue> vars = new LinkedHashMap<>();
        if (variables != null) {
            variables.forEach((k, v) -> {
                if (k.startsWith("principal.")) {
                    vars.put(k, v);
                }
            });
        }
        return vars;
    }
}
