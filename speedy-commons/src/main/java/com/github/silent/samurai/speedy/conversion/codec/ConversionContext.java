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
/// {@link JavaTypeRegistry} and {@link DbConversionRegistry}. User-supplied
/// type modules ({@code SpeedyTypeModule}) extend these afterwards via
/// {@link #forType}.
///
/// @see SpeedyContext
/// @see JavaTypeRegistry
/// @see DbConversionRegistry
public final class ConversionContext extends SpeedyContext {

    /// Creates a {@code ConversionContext} pre-populated with
    /// {@link JavaTypeRegistry} and {@link DbConversionRegistry}.
    /// Format-level type registrations are contributed afterwards via
    /// {@code ISpeedyConfiguration#typeModules()}.
    public static ConversionContext withDefaults() {
        ConversionContext ctx = new ConversionContext();
        ctx.put(JavaTypeRegistry.defaults());
        ctx.put(new DbConversionRegistry());
        return ctx;
    }

    /// Returns a fluent {@link TypeBuilder} for registering a custom Java type
    /// into {@link JavaTypeRegistry}.
    public <T> TypeBuilder<T> forType(Class<T> type) {
        return new TypeBuilder<>(this, type);
    }
}
