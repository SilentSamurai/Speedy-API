package com.github.silent.samurai.speedy.conversion.codec;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.conversion.ext.TypeBuilder;
import com.github.silent.samurai.speedy.conversion.registry.JavaTypeRegistry;
import com.github.silent.samurai.speedy.conversion.registry.DbConversionRegistry;

/// Container for codec registries used during request processing.
///
/// ## Design Philosophy
/// Extends {@link SpeedyContext} which provides the generic typed-bag storage.
/// This subclass populates the bag with the built-in registries:
/// {@link JavaTypeRegistry} and {@link DbConversionRegistry}.
/// Format-specific registries (e.g. JsonRegistry from speedy-json-io) are
/// contributed later via {@link com.github.silent.samurai.speedy.interfaces.ISpeedyIoProvider}
/// discovered through ServiceLoader so that speedy-core stays free of
/// compile-time dependencies on format modules.
///
/// @see SpeedyContext
/// @see JavaTypeRegistry
/// @see DbConversionRegistry
public final class ConversionContext extends SpeedyContext {

    /// Creates a {@code ConversionContext} pre-populated with
    /// {@link JavaTypeRegistry} and {@link DbConversionRegistry}.
    /// Format-specific registries are contributed via SPI afterwards.
    public static ConversionContext withDefaults() {
        ConversionContext ctx = new ConversionContext();
        ctx.put(JavaTypeRegistry.defaults());
        ctx.put(new DbConversionRegistry());
        return ctx;
    }

    /// Returns a fluent {@link TypeBuilder} for registering a custom Java type
    /// across all three registries (JavaTypeRegistry, JsonRegistry,
    /// DbConversionRegistry).
    public <T> TypeBuilder<T> forType(Class<T> type) {
        return new TypeBuilder<>(this, type);
    }
}
