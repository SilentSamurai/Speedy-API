package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ConversionRegistry<K> {

    private final Map<K, Codec> codecs = new HashMap<>();
    private final ConversionRegistry<K> parent;

    public ConversionRegistry(ConversionRegistry<K> parent) {
        this.parent = parent;
    }

    public ConversionRegistry<K> register(K key,
                                          Function<SpeedyValue, Object> encode,
                                          Function<Object, SpeedyValue> decode) {
        codecs.put(key, new Codec(encode, decode));
        return this;
    }

    protected Codec lookup(K key) {
        Codec c = codecs.get(key);
        if (c != null) return c;
        return parent != null ? parent.lookup(key) : null;
    }

    /// Public accessor for looking up a codec by key.
    /// Delegates to the internal {@link #lookup} method so external
    /// callers (e.g. serializers in other packages) can retrieve
    /// registered codecs without gaining access to the protected lookup.
    ///
    /// @param key the registration key
    /// @return the matching Codec, or null if not found
    public Codec getCodec(K key) {
        return lookup(key);
    }

    protected ConversionRegistry<K> getParent() {
        return parent;
    }
}
