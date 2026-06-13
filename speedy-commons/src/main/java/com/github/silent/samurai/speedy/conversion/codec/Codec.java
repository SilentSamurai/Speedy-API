package com.github.silent.samurai.speedy.conversion.codec;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import java.util.function.Function;

/// A directional converter between a **single** Java class `T` and `SpeedyValue`.
///
/// ## Design Philosophy
/// Every {@code Codec<T>} handles exactly **one** Java class `T` — no `instanceof`
/// branching, no multi-type dispatch. The `Class<T>` type token is carried at
/// runtime so that {@link #safeDecode(Object)} can verify, via {@link Class#cast},
/// that the raw value is actually of type `T` before the inner decode function
/// is invoked. This eliminates blind casts and ensures type mismatches are caught
/// at the codec boundary with a descriptive exception.
///
/// - **encode** — converts {@code SpeedyValue → T} (SpeedyValue → Java)
/// - **decode** — converts {@code T → SpeedyValue} (Java → SpeedyValue)
///
/// If multiple Java classes map to the same SpeedyValue type (e.g. `Boolean` and
/// `String` both mapping to `SpeedyBoolean`), register separate {@code Codec}
/// instances under the same {@code ValueType} in the registry.
///
/// @param <T> the Java class handled by this codec
public record Codec<T>(
        Class<T> type,
        Function<SpeedyValue, T> encode,
        Function<T, SpeedyValue> decode
) {
    /// Encodes a {@code SpeedyValue} into the Java type `T`, returning {@code null}
    /// when the input is {@code null} or {@link SpeedyNull}.
    public T safeEncode(SpeedyValue sv) {
        if (sv == null || sv instanceof SpeedyNull) return null;
        return encode.apply(sv);
    }

    /// Decodes a raw Java object into a {@code SpeedyValue}.
    /// Uses {@link Class#cast} to verify at runtime that the input is actually of
    /// type `T` before calling the inner decode function.
    public SpeedyValue safeDecode(Object raw) {
        if (raw == null) return SpeedyNull.SPEEDY_NULL;
        return decode.apply(type.cast(raw));
    }
}
