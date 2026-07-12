package com.github.silent.samurai.speedy.policy.condition;

import java.util.Map;
import java.util.Optional;

/// A format-neutral view over one parsed {@code conditions[]} entry, handed to a
/// {@link PolicyConditionFactory} so condition types stay decoupled from the JSON library the
/// document parser happens to use.
public final class ConditionSpec {

    private final Map<String, Object> raw;

    public ConditionSpec(Map<String, Object> raw) {
        this.raw = raw == null ? Map.of() : raw;
    }

    public String type() {
        Object type = raw.get("type");
        return type == null ? null : String.valueOf(type);
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(raw.get(key));
    }

    public String getString(String key) {
        Object v = raw.get(key);
        return v == null ? null : String.valueOf(v);
    }

    public Map<String, Object> asMap() {
        return raw;
    }
}
