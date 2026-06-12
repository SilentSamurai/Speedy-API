package com.github.silent.samurai.speedy.mappings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ConversionContext {

    private final Map<Class<?>, ConversionRegistry<?>> registries = new HashMap<>();

    public static ConversionContext withDefaults() {
        ConversionContext ctx = new ConversionContext();
        ctx.put(JavaTypeRegistry.class, JavaTypeRegistry.defaults());
        ctx.put(JsonRegistry.class, JsonRegistry.defaults());
        ctx.put(DbConversionRegistry.class, new DbConversionRegistry(null));
        return ctx;
    }

    public <R extends ConversionRegistry<?>> ConversionContext put(Class<R> key, R registry) {
        registries.put(key, registry);
        return this;
    }

    public ConversionContext put(ConversionRegistry<?> registry) {
        registries.put(registry.getClass(), registry);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <R extends ConversionRegistry<?>> R get(Class<R> type) {
        R r = (R) registries.get(type);
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
