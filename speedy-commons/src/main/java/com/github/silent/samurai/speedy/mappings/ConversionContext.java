package com.github.silent.samurai.speedy.mappings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/// Container for the three codec registries used during request processing.
///
/// ## Design Philosophy
/// Each registry is stored by its concrete class as key. The {@link #get(Class)}
/// method provides unchecked retrieval — the caller is responsible for knowing
/// which registry type they need. This avoids a shared base class that would
/// conflate fundamentally different lookup strategies.
///
/// @see JavaTypeRegistry  keyed by Java class
/// @see JsonRegistry      keyed by (ValueType, Java class)
/// @see DbConversionRegistry  keyed by (ColumnType, Java class)
public final class ConversionContext {

    private final Map<Class<?>, Object> registries = new HashMap<>();

    public static ConversionContext withDefaults() {
        ConversionContext ctx = new ConversionContext();
        ctx.put(JavaTypeRegistry.class, JavaTypeRegistry.defaults());
        ctx.put(JsonRegistry.class, JsonRegistry.defaults());
        ctx.put(DbConversionRegistry.class, new DbConversionRegistry());
        return ctx;
    }

    public <R> ConversionContext put(Class<R> key, R registry) {
        registries.put(key, registry);
        return this;
    }

    public <R> R get(Class<R> type) {
        R r = type.cast(registries.get(type));
        if (r == null) throw new IllegalStateException("No registry for type: " + type.getName());
        return r;
    }

    public boolean has(Class<?> type) {
        return registries.containsKey(type);
    }

    public Set<Class<?>> types() {
        return registries.keySet();
    }

    public <T> TypeBuilder<T> forType(Class<T> type) {
        return new TypeBuilder<>(this, type);
    }
}
