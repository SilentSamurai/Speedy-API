package com.github.silent.samurai.speedy.context;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Typed bag that stores objects keyed by their concrete class.
///
/// ## Design Philosophy,
/// Rather than passing multiple registries or services separately, this bag
/// groups them into a single container. The key is always the runtime class of
/// the stored value, so callers retrieve entries via {@link #get(Class)} without
/// needing to know the key ahead of time. A {@link Objects#requireNonNull}
/// guard prevents null entries from entering the map, ensuring that
/// {@link #get(Class)} never returns null — it either returns a non-null entry
/// or throws.
///
/// @see ConversionContext the conversion-specific subclass
public class SpeedyContext {

    /// Internal storage: class → instance.
    private final Map<Class<?>, Object> items = new HashMap<>();

    /// Stores a value keyed by its runtime class.
    /// The value must not be null.
    public <R> SpeedyContext put(R value) {
        Objects.requireNonNull(value, "value must not be null");
        items.put(value.getClass(), value);
        return this;
    }

    /// Stores a value under an explicit key type.
    /// Use when the value's runtime class is a subtype of the desired lookup key.
    public <K, V extends K> SpeedyContext put(Class<K> key, V value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        items.put(key, value);
        return this;
    }

    /// Retrieves a value by its key type.
    /// Throws if no value is registered for the given type.
    public <R> R get(Class<R> type) {
        R r = type.cast(items.get(type));
        if (r == null) {
            throw new IllegalStateException("No entry for type: " + type.getName());
        }
        return r;
    }

    /// Checks whether a value exists for the given type.
    public boolean has(Class<?> type) {
        return items.containsKey(type);
    }

    /// Returns all stored key types.
    public Set<Class<?>> types() {
        return items.keySet();
    }
}
