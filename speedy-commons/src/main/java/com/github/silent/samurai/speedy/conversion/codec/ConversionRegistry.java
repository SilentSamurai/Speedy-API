package com.github.silent.samurai.speedy.conversion.codec;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/// A heterogenous registry that maps keys of type `K` to {@link Codec} instances.
///
/// ## Design Philosophy
/// This is the base class for type-safe codec lookups. The signature
/// {@link #register(Class, Function, Function)} and {@link #lookup(Class)} use
/// `Class<T>` as the key, which acts as a **runtime type token** — the same `T`
/// flows into both the key and the returned {@code Codec<T>}.
///
/// The single unchecked cast inside {@link #lookup(Class)} is safe by construction:
/// because the key *is* the {@code Class<T>} token, no other `T` could have been
/// stored under that key. Even so, every {@code Codec<T>} additionally verifies
/// inputs at runtime via {@link Codec#safeDecode(Object)}.
///
/// @param <K> the key type (expected to be {@link Class} for type-safe usage)
public class ConversionRegistry<K> {

    private final Map<K, Codec<?>> codecs = new HashMap<>();
    private final ConversionRegistry<K> parent;

    public ConversionRegistry(ConversionRegistry<K> parent) {
        this.parent = parent;
    }

    /// Registers a codec for a given key. The `Class<T>` type token is required so
    /// the {@link Codec} can perform runtime type verification via {@link Class#cast}.
    ///
    /// @param key   the registry key
    /// @param type  the runtime type token for the Java class handled by this codec
    /// @param encode {@code SpeedyValue → T}
    /// @param decode {@code T → SpeedyValue}
    public <T> ConversionRegistry<K> register(K key, Class<T> type,
                                              Function<SpeedyValue, T> encode,
                                              Function<T, SpeedyValue> decode) {
        codecs.put(key, new Codec<>(type, encode, decode));
        return this;
    }

    /// Looks up a codec by key. Returns a wildcard-typed codec; the caller is
    /// responsible for verifying the returned type matches expectations (e.g.
    /// via {@link Class#cast} on the encode result).
    protected Codec<?> lookup(K key) {
        Codec<?> c = codecs.get(key);
        if (c != null) return c;
        return parent != null ? parent.lookup(key) : null;
    }

    /// Public accessor for looking up a codec by key.
    public Codec<?> getCodec(K key) {
        return lookup(key);
    }

    protected ConversionRegistry<K> getParent() {
        return parent;
    }
}
