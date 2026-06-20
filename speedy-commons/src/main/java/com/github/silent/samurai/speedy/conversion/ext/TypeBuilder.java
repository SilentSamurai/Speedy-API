package com.github.silent.samurai.speedy.conversion.ext;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.conversion.registry.JavaTypeRegistry;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyText;

import java.util.function.Function;

/// Fluent builder for registering a custom Java type into {@link JavaTypeRegistry}.
///
/// ## Design Philosophy
/// {@link JavaTypeRegistry} is the only registry a library user ever needs to touch.
/// It drives every path that involves a custom type: request parsing, response
/// serialization, URL query-parameter conversion, and the event system
/// ({@link com.github.silent.samurai.speedy.events.EventProcessor} uses
/// {@link com.github.silent.samurai.speedy.conversion.walker.java.SpeedyToJava} and
/// {@link com.github.silent.samurai.speedy.conversion.walker.java.JavaToSpeedy} to
/// convert {@code SpeedyEntity ↔ POJO}).
///
/// The other registries ({@code ApiIoRegistry}, {@code DbConversionRegistry}) are
/// owned by format implementors and query processors respectively — users never
/// register into them.
///
/// @param <T> the Java type being registered
public class TypeBuilder<T> {

    private final ConversionContext ctx;
    private final Class<T> type;

    public TypeBuilder(ConversionContext ctx, Class<T> type) {
        this.ctx = ctx;
        this.type = type;
    }

    /// Registers a text-backed codec for types whose natural representation is a
    /// {@code String} (e.g. {@code Email}, {@code PhoneNumber}, {@code IBAN}).
    ///
    /// Internally creates a {@link com.github.silent.samurai.speedy.conversion.codec.Codec}
    /// that bridges through {@link SpeedyText}:
    /// - encode: {@code SpeedyValue → T} via {@code fromStr.apply(sv.asText())}
    /// - decode: {@code T → SpeedyValue} via {@code new SpeedyText(toStr.apply(raw))}
    ///
    /// Sufficient for JSON request/response, DB persistence (via JPA {@code @Convert}),
    /// and the event system without any further configuration.
    public TypeBuilder<T> asText(Function<T, String> toStr, Function<String, T> fromStr) {
        ctx.get(JavaTypeRegistry.class).register(type,
                sv -> fromStr.apply(sv.asText()),
                raw -> new SpeedyText(toStr.apply(raw)));
        return this;
    }

    /// Registers a raw {@link com.github.silent.samurai.speedy.conversion.codec.Codec}
    /// for types that are not text-backed (e.g. a type whose internal SpeedyValue
    /// representation is {@code SpeedyInt} or {@code SpeedyDouble}).
    ///
    /// Use this when {@link #asText} is not appropriate — i.e. when the type does not
    /// have a meaningful {@code toString} / {@code fromString} contract.
    public TypeBuilder<T> codec(Function<SpeedyValue, T> enc, Function<T, SpeedyValue> dec) {
        ctx.get(JavaTypeRegistry.class).register(type, enc, dec);
        return this;
    }
}
