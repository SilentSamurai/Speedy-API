package com.github.silent.samurai.speedy.conversion.registry;

import com.github.silent.samurai.speedy.conversion.codec.Codec;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/// Stores codecs for converting between JDBC column values and {@link SpeedyValue}
/// using a compound key of {@code (ColumnType, Class<?>)}.
///
/// ## Design Philosophy
/// This is a pure registry — it stores and looks up codecs but does not perform
/// routing logic (enum handling, null checks, ValueType dispatch). That belongs
/// in the walker: {@link com.github.silent.samurai.speedy.conversion.walker.db.DbConverter}.
///
/// Each codec handles exactly one Java class — no {@code instanceof} branching
/// inside decode lambdas. Registries can be chained via the parent constructor
/// to form a fallback hierarchy: user-registered codecs as parent, built-in
/// JDBC codecs (e.g. from {@code JooqConverters}) layered on top.
///
/// @see Codec
/// @see com.github.silent.samurai.speedy.conversion.walker.db.DbConverter
public class DbConversionRegistry {

    private final DbConversionRegistry parent;
    private final Map<ColumnType, Map<Class<?>, Codec<?>>> codecs = new HashMap<>();

    public DbConversionRegistry() {
        this.parent = null;
    }

    public DbConversionRegistry(DbConversionRegistry parent) {
        this.parent = parent;
    }

    /// Registers a codec that converts between a single Java class `T` and a
    /// SpeedyValue for the given {@link ColumnType}.
    ///
    /// @param col    the column type
    /// @param type   the Java class handled by this codec
    /// @param encode {@code SpeedyValue → T}
    /// @param decode {@code T → SpeedyValue}
    public <T> DbConversionRegistry register(ColumnType col, Class<T> type,
                                             Function<SpeedyValue, T> encode,
                                             Function<T, SpeedyValue> decode) {
        codecs.computeIfAbsent(col, k -> new HashMap<>()).put(type, new Codec<>(type, encode, decode));
        return this;
    }

    /// Looks up a codec by ColumnType and Java class, walking supertypes if no
    /// exact match is found, then falling back to the parent registry.
    public Codec<?> findCodec(ColumnType col, Class<?> clazz) {
        Map<Class<?>, Codec<?>> byClass = codecs.get(col);
        if (byClass != null) {
            Codec<?> c = byClass.get(clazz);
            if (c != null) return c;
            for (Map.Entry<Class<?>, Codec<?>> e : byClass.entrySet()) {
                if (e.getKey().isAssignableFrom(clazz)) return e.getValue();
            }
        }
        if (parent != null) return parent.findCodec(col, clazz);
        return null;
    }
}
