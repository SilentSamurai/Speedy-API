package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.io.JsonToSpeedy;
import com.github.silent.samurai.speedy.io.SpeedyToJson;
import com.github.silent.samurai.speedy.models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/// Converts between Jackson JSON types and {@link SpeedyValue} using a
/// compound key of {@code (ValueType, Class<?>)}.
///
/// ## Design Philosophy
/// Unlike a generic {@link ConversionRegistry}, this registry stores codecs under
/// a compound key: the SpeedyValue type ({@link ValueType}) **and** the Java class
/// of the raw JSON value. Each codec handles exactly one Java class — no
/// {@code instanceof} branching inside decode lambdas.
///
/// **Decode** (JSON → SpeedyValue): looks up by {@code (ValueType, raw.getClass())}
/// and walks the class hierarchy if no exact match is found.
///
/// **Encode** (SpeedyValue → JSON): uses a per-{@code ValueType} default target
/// class (e.g. {@code BOOL → Boolean.class}, {@code INT → Long.class}) to select
/// the appropriate codec.
///
/// @see Codec
/// @see JsonToSpeedy
/// @see SpeedyToJson
public class JsonRegistry {

    /// Compound storage: ValueType → { Java class → Codec }
    private final Map<ValueType, Map<Class<?>, Codec<?>>> codecs = new HashMap<>();

    /// Default target Java class when encoding a SpeedyValue for JSON output.
    private static final Map<ValueType, Class<?>> DEFAULT_ENCODE_TYPE = Map.ofEntries(
            Map.entry(ValueType.BOOL, Boolean.class),
            Map.entry(ValueType.TEXT, String.class),
            Map.entry(ValueType.INT, Long.class),
            Map.entry(ValueType.FLOAT, Double.class),
            Map.entry(ValueType.DATE, String.class),
            Map.entry(ValueType.TIME, String.class),
            Map.entry(ValueType.DATE_TIME, String.class),
            Map.entry(ValueType.ZONED_DATE_TIME, String.class),
            Map.entry(ValueType.ENUM, String.class),
            Map.entry(ValueType.ENUM_ORD, Long.class)
    );

    public JsonRegistry() {
    }

    /// Registers a codec that converts between a single Java class `T` and a
    /// SpeedyValue of the given {@link ValueType}.
    ///
    /// @param vt     the SpeedyValue type
    /// @param type   the Java class handled by this codec
    /// @param encode {@code SpeedyValue → T}
    /// @param decode {@code T → SpeedyValue}
    public <T> JsonRegistry register(ValueType vt, Class<T> type,
                                     Function<SpeedyValue, T> encode,
                                     Function<T, SpeedyValue> decode) {
        codecs.computeIfAbsent(vt, k -> new HashMap<>()).put(type, new Codec<>(type, encode, decode));
        return this;
    }

    /// Decodes a raw JSON value into a SpeedyValue by looking up the codec
    /// matching the given {@code ValueType} and the runtime class of {@code raw}.
    /// Walks the class hierarchy if no exact match is found.
    public SpeedyValue decode(ValueType vt, Object raw) throws BadRequestException {
        if (raw == null) return SpeedyNull.SPEEDY_NULL;
        Codec<?> codec = findCodec(vt, raw.getClass());
        if (codec != null) {
            return codec.safeDecode(raw);
        }
        throw new BadRequestException("No codec found for " + vt + " from " + raw.getClass().getSimpleName());
    }

    /// Encodes a SpeedyValue into its default JSON Java representation.
    /// Returns {@code null} if no codec is registered for the value type.
    public Object encode(ValueType vt, SpeedyValue sv) {
        if (sv == null || sv instanceof SpeedyNull) return null;
        Class<?> targetType = DEFAULT_ENCODE_TYPE.get(vt);
        if (targetType == null) return null;
        Codec<?> codec = findCodec(vt, targetType);
        if (codec != null) {
            return codec.safeEncode(sv);
        }
        return null;
    }

    /// Finds a codec for the given ValueType and a Java class, walking supertypes
    /// if no exact match is found. The codec carries its own Class<T> token for
    /// runtime verification during {@link Codec#safeDecode(Object)}.
    private Codec<?> findCodec(ValueType vt, Class<?> clazz) {
        Map<Class<?>, Codec<?>> byClass = codecs.get(vt);
        if (byClass == null) return null;
        // Exact match
        Codec<?> c = byClass.get(clazz);
        if (c != null) return c;
        // Walk supertypes
        for (Map.Entry<Class<?>, Codec<?>> e : byClass.entrySet()) {
            if (e.getKey().isAssignableFrom(clazz)) return e.getValue();
        }
        return null;
    }

    public static JsonRegistry defaults() {
        JsonRegistry r = new JsonRegistry();

        r.register(ValueType.BOOL, Boolean.class,
                sv -> ((SpeedyBoolean) sv).getValue(),
                SpeedyBoolean::new);

        r.register(ValueType.TEXT, String.class,
                sv -> ((SpeedyText) sv).getValue(),
                SpeedyText::new);

        r.register(ValueType.INT, Long.class,
                sv -> ((SpeedyInt) sv).getValue(),
                SpeedyInt::new);

        r.register(ValueType.FLOAT, Double.class,
                sv -> ((SpeedyDouble) sv).getValue(),
                SpeedyDouble::new);

        r.register(ValueType.DATE, String.class,
                sv -> ((SpeedyDate) sv).getValue().format(DateTimeFormatter.ISO_DATE),
                s -> {
                    LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_DATE);
                    return new SpeedyDate(d);
                });

        r.register(ValueType.TIME, String.class,
                sv -> ((SpeedyTime) sv).getValue().format(DateTimeFormatter.ISO_TIME),
                s -> {
                    LocalTime t = LocalTime.parse(s, DateTimeFormatter.ISO_TIME);
                    return new SpeedyTime(t);
                });

        r.register(ValueType.DATE_TIME, String.class,
                sv -> ((SpeedyDateTime) sv).getValue().format(DateTimeFormatter.ISO_DATE_TIME),
                s -> {
                    LocalDateTime dt = LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
                    return new SpeedyDateTime(dt);
                });

        r.register(ValueType.ZONED_DATE_TIME, String.class,
                sv -> ((SpeedyZonedDateTime) sv).asZonedDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                s -> {
                    ZonedDateTime zdt = ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    return new SpeedyZonedDateTime(zdt);
                });

        r.register(ValueType.ENUM, String.class,
                SpeedyValue::asEnum,
                SpeedyText::new);

        r.register(ValueType.ENUM_ORD, Long.class,
                SpeedyValue::asEnumOrd,
                SpeedyInt::new);

        return r;
    }
}
