package com.github.silent.samurai.speedy.conversion.codec;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.conversion.ext.TypeBuilder;
import com.github.silent.samurai.speedy.conversion.registry.JavaTypeRegistry;
import com.github.silent.samurai.speedy.conversion.registry.JsonRegistry;
import com.github.silent.samurai.speedy.conversion.registry.DbConversionRegistry;

/// Container for the three codec registries used during request processing.
///
/// ## Design Philosophy
/// Extends {@link SpeedyContext} which provides the generic typed-bag storage.
/// This subclass populates the bag with the three registries needed for
/// conversion: {@link JavaTypeRegistry}, {@link JsonRegistry}, and
/// {@link DbConversionRegistry}. The inherited {@link #get(Class)} method
/// provides unchecked retrieval — the caller is responsible for knowing which
/// registry type they need. This avoids a shared base class that would conflate
/// fundamentally different lookup strategies.
///
/// @see SpeedyContext
/// @see JavaTypeRegistry
/// @see JsonRegistry
/// @see DbConversionRegistry
public final class ConversionContext extends SpeedyContext {

    /// Creates a {@code ConversionContext} populated with the three default
    /// registries: {@link JavaTypeRegistry}, {@link JsonRegistry}, and
    /// {@link DbConversionRegistry}.
    public static ConversionContext withDefaults() {
        ConversionContext ctx = new ConversionContext();
        ctx.put(JavaTypeRegistry.defaults());
        ctx.put(JsonRegistry.defaults());
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
