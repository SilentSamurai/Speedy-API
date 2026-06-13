package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyText;

import java.util.function.Function;

/// Fluent builder for registering a custom Java type across all three registries.
///
/// ## Design Philosophy
/// Each registration call explicitly specifies the Java class via the builder's
/// type parameter `T`. The compiler checks that `encode` and `decode` agree on
/// `T` at the registration site. Internally, the registries store the
/// {@link Codec} with a {@link Class#cast} runtime guard, so that even if a
/// <strong>lookup</strong> path were to return the wrong codec, the
/// {@code safeDecode}/{@code safeEncode} methods would catch it at the boundary.
///
/// @param <T> the Java type being registered
public class TypeBuilder<T> {

    private final ConversionContext ctx;
    private final Class<T> type;

    TypeBuilder(ConversionContext ctx, Class<T> type) {
        this.ctx = ctx;
        this.type = type;
    }

    /// Registers a TEXT-based codec in both the JavaTypeRegistry and JsonRegistry.
    /// The encode/decode lambdas convert between `T` and `String`.
    public TypeBuilder<T> asText(Function<T, String> toStr, Function<String, T> fromStr) {
        if (ctx.has(JavaTypeRegistry.class)) {
            JavaTypeRegistry jtr = ctx.get(JavaTypeRegistry.class);
            jtr.register(type,
                    sv -> fromStr.apply(sv.asText()),
                    raw -> new SpeedyText(toStr.apply(raw)));
        }
        if (ctx.has(JsonRegistry.class)) {
            JsonRegistry jr = ctx.get(JsonRegistry.class);
            jr.register(ValueType.TEXT, String.class,
                    sv -> sv.asText(),
                    raw -> new SpeedyText(raw));
        }
        return this;
    }

    /// Registers a codec for a DB column type.
    public TypeBuilder<T> onDb(ColumnType col,
                               Function<SpeedyValue, T> enc,
                               Function<T, SpeedyValue> dec) {
        if (ctx.has(DbConversionRegistry.class)) {
            ctx.get(DbConversionRegistry.class).register(col, type, enc, dec);
        }
        return this;
    }

    /// Registers a codec for a JSON value type.
    public TypeBuilder<T> onJson(ValueType vt,
                                 Function<SpeedyValue, T> enc,
                                 Function<T, SpeedyValue> dec) {
        if (ctx.has(JsonRegistry.class)) {
            ctx.get(JsonRegistry.class).register(vt, type, enc, dec);
        }
        return this;
    }

    /// Registers a variant codec in the JavaTypeRegistry (keyed by ValueType).
    public TypeBuilder<T> onJava(ValueType vt,
                                 Function<SpeedyValue, T> enc,
                                 Function<T, SpeedyValue> dec) {
        if (ctx.has(JavaTypeRegistry.class)) {
            ctx.get(JavaTypeRegistry.class).register(type, vt, enc, dec);
        }
        return this;
    }

}
