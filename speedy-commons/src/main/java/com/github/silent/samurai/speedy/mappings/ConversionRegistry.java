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

    public Codec lookup(K key) {
        Codec c = codecs.get(key);
        if (c != null) return c;
        return parent != null ? parent.lookup(key) : null;
    }

    protected ConversionRegistry<K> getParent() {
        return parent;
    }
}
