package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/// Converts between `SpeedyValue` and plain Java POJO types using a two-tier
/// codec lookup strategy.
///
/// ## Design Philosophy
/// The primary key for every codec is a **single Java class** (`Class<T>`). Each
/// codec handles exactly one Java type — no `instanceof` branching. When a Java
/// type needs multiple SpeedyValue representations (e.g. `BigDecimal` as both
/// `FLOAT` and `INT`), separate variant codecs are registered under the same
/// class key with distinct {@link ValueType} discriminators.
///
/// Lookups are type-safe by construction: the `Class<T>` key acts as a runtime
/// type token, and every codec additionally verifies inputs via
/// {@link Codec#safeDecode(Object)} / {@link Codec#safeEncode(SpeedyValue)}.
///
/// @see Codec
/// @see ConversionRegistry
public class JavaTypeRegistry extends ConversionRegistry<Class<?>> {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(
            int.class, Integer.class,
            long.class, Long.class,
            double.class, Double.class,
            float.class, Float.class,
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            short.class, Short.class,
            void.class, Void.class
    );

    /// Variant codecs: one Java class may have multiple codecs keyed by ValueType.
    /// E.g. `BigDecimal.class → { FLOAT → Codec<BigDecimal>, INT → Codec<BigDecimal> }`.
    private final Map<Class<?>, Map<ValueType, Codec<?>>> vtCodecs = new HashMap<>();

    /// Per-type converters that parse a string literal into a Java object.
    private final Map<Class<?>, Function<String, ?>> fromStringConverters = new HashMap<>();

    public JavaTypeRegistry(JavaTypeRegistry parent) {
        super(parent);
    }

    private static Class<?> wrap(Class<?> clazz) {
        return clazz.isPrimitive() ? PRIMITIVE_TO_WRAPPER.getOrDefault(clazz, clazz) : clazz;
    }

    private static Enum convertEnumFromSpeedy(SpeedyValue sv, Class<?> enumClass) {
        if (sv.isText()) {
            return findEnumConstant(enumClass, sv.asText());
        }
        if (sv.isEnum()) {
            return findEnumConstant(enumClass, sv.asEnum());
        }
        if (sv.isEnumOrd()) {
            int ord = sv.asEnumOrd().intValue();
            Enum[] constants = (Enum[]) enumClass.getEnumConstants();
            if (constants != null && ord >= 0 && ord < constants.length) {
                return constants[ord];
            }
        }
        throw new ConversionException("Cannot convert " + sv + " to enum " + enumClass.getSimpleName());
    }

    private static Enum findEnumConstant(Class<?> enumClass, String name) {
        Enum[] constants = (Enum[]) enumClass.getEnumConstants();
        if (constants != null) {
            for (Enum constant : constants) {
                if (constant.name().equals(name)) {
                    return constant;
                }
            }
        }
        throw new ConversionException("Invalid enum constant '" + name + "' for " + enumClass.getSimpleName());
    }

    /// Casts a value to the target type with runtime verification.
    /// Uses {@code wrap(targetClass)} for the runtime check so primitives are handled
    /// via their wrapper class (since {@link Class#cast} doesn't support primitives).
    /// The {@code safeEncode} / {@code safeDecode} inside each codec already verify
    /// the type, so this is an additional boundary assertion.
    private static <T> T checkedCast(Object value, Class<T> targetClass) {
        Class<?> wrapped = wrap(targetClass);
        wrapped.cast(value);
        if (targetClass.isPrimitive()) {
            return (T) value;
        }
        return targetClass.cast(value);
    }

    private static SpeedyValue convertEnumToSpeedy(Enum<?> en, ValueType vt) {
        return switch (vt) {
            case TEXT, ENUM -> new SpeedyText(en.name());
            case INT, ENUM_ORD -> new SpeedyInt((long) en.ordinal());
            default ->
                    throw new ConversionException("Cannot convert enum " + en.getClass().getSimpleName() + " to " + vt);
        };
    }

    public static JavaTypeRegistry defaults() {
        JavaTypeRegistry r = new JavaTypeRegistry(null);

        r.register(String.class,
                sv -> ((SpeedyText) sv).getValue(),
                raw -> new SpeedyText(raw));

        r.register(UUID.class,
                sv -> UUID.fromString(sv.asText()),
                uuid -> new SpeedyText(uuid.toString()));

        r.register(Boolean.class,
                sv -> ((SpeedyBoolean) sv).getValue(),
                SpeedyBoolean::new);

        r.register(Long.class,
                sv -> ((SpeedyInt) sv).getValue(),
                raw -> new SpeedyInt(raw));

        r.register(Integer.class,
                sv -> ((SpeedyInt) sv).getValue().intValue(),
                val -> new SpeedyInt(val.longValue()));

        r.register(BigInteger.class,
                sv -> BigInteger.valueOf(((SpeedyInt) sv).getValue()),
                bi -> new SpeedyInt(bi.longValue()));

        r.register(Double.class,
                sv -> ((SpeedyDouble) sv).getValue(),
                SpeedyDouble::new);

        r.register(Float.class,
                sv -> ((SpeedyDouble) sv).getValue().floatValue(),
                f -> new SpeedyDouble(f.doubleValue()));

        r.register(BigDecimal.class,
                sv -> BigDecimal.valueOf(((SpeedyDouble) sv).getValue()),
                bd -> new SpeedyDouble(bd.doubleValue()));

        r.register(BigDecimal.class, ValueType.FLOAT,
                sv -> BigDecimal.valueOf(((SpeedyDouble) sv).getValue()),
                bd -> new SpeedyDouble(bd.doubleValue()));

        r.register(BigDecimal.class, ValueType.INT,
                sv -> BigDecimal.valueOf(((SpeedyInt) sv).getValue().longValue()),
                bd -> new SpeedyInt(bd.longValue()));

        r.register(BigInteger.class, ValueType.FLOAT,
                sv -> BigInteger.valueOf(((SpeedyDouble) sv).getValue().longValue()),
                bi -> new SpeedyDouble(bi.doubleValue()));

        r.register(LocalDate.class,
                sv -> ((SpeedyDate) sv).asDate(),
                SpeedyDate::new);

        r.register(java.util.Date.class,
                sv -> {
                    Instant instant = ((SpeedyDate) sv).getValue().atStartOfDay(ZoneId.of("UTC")).toInstant();
                    return java.util.Date.from(instant);
                },
                d -> new SpeedyDate(LocalDate.ofInstant(d.toInstant(), ZoneId.of("UTC"))));

        r.register(java.sql.Date.class,
                sv -> java.sql.Date.valueOf(((SpeedyDate) sv).getValue()),
                sql -> new SpeedyDate(sql.toLocalDate()));

        r.register(LocalTime.class,
                sv -> ((SpeedyTime) sv).asTime(),
                raw -> new SpeedyTime(raw));

        r.register(LocalDateTime.class,
                sv -> ((SpeedyDateTime) sv).getValue(),
                raw -> new SpeedyDateTime(raw));

        r.register(Instant.class, ValueType.DATE_TIME,
                sv -> ((SpeedyDateTime) sv).getValue().atZone(ZoneId.of("UTC")).toInstant(),
                instant -> new SpeedyDateTime(LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))));

        r.register(Instant.class, ValueType.ZONED_DATE_TIME,
                sv -> ((SpeedyZonedDateTime) sv).asZonedDateTime().toInstant(),
                instant -> new SpeedyZonedDateTime(instant.atZone(ZoneId.of("UTC"))));

        r.register(Timestamp.class,
                sv -> Timestamp.valueOf(((SpeedyDateTime) sv).getValue()),
                ts -> new SpeedyDateTime(ts.toLocalDateTime()));

        r.register(ZonedDateTime.class,
                sv -> ((SpeedyZonedDateTime) sv).asZonedDateTime(),
                raw -> new SpeedyZonedDateTime(raw));

        r.registerFromString(Integer.class, Integer::parseInt);
        r.registerFromString(Long.class, Long::parseLong);
        r.registerFromString(Short.class, Short::parseShort);
        r.registerFromString(Byte.class, Byte::parseByte);
        r.registerFromString(Float.class, Float::parseFloat);
        r.registerFromString(Double.class, Double::parseDouble);
        r.registerFromString(Boolean.class, Boolean::parseBoolean);
        r.registerFromString(String.class, v -> v);
        r.registerFromString(UUID.class, UUID::fromString);
        r.registerFromString(LocalDate.class, LocalDate::parse);
        r.registerFromString(LocalDateTime.class, LocalDateTime::parse);
        r.registerFromString(LocalTime.class, LocalTime::parse);
        r.registerFromString(ZonedDateTime.class, ZonedDateTime::parse);
        r.registerFromString(Instant.class, Instant::parse);

        return r;
    }

    /// Registers a primary codec for a Java class. The key (and type token) is the
    /// class itself.
    public <T> JavaTypeRegistry register(Class<T> clazz,
                                         Function<SpeedyValue, T> encode,
                                         Function<T, SpeedyValue> decode) {
        super.register(clazz, clazz, encode, decode);
        return this;
    }

    /// Registers a variant codec for a Java class qualified by a specific ValueType.
    /// Used when a single Java type can map to different SpeedyValue representations
    /// (e.g. `BigDecimal` as both `FLOAT` and `INT`).
    public <T> JavaTypeRegistry register(Class<T> clazz, ValueType vt,
                                         Function<SpeedyValue, T> encode,
                                         Function<T, SpeedyValue> decode) {
        vtCodecs.computeIfAbsent(clazz, k -> new HashMap<>()).put(vt, new Codec<>(clazz, encode, decode));
        return this;
    }

    /// Registers a from-string converter.
    public <T> JavaTypeRegistry registerFromString(Class<T> clazz, Function<String, T> converter) {
        fromStringConverters.put(clazz, converter);
        return this;
    }

    /// Parses a string literal into the requested Java type.
    public <T> T parseString(String literal, Class<T> target) throws SpeedyHttpException {
        if (literal == null) return null;
        Class<?> wrapped = wrap(target);
        Function<String, ?> fn = fromStringConverters.get(wrapped);
        if (fn == null && getParent() instanceof JavaTypeRegistry p) {
            fn = p.fromStringConverters.get(wrapped);
        }
        if (fn != null) {
            return checkedCast(fn.apply(literal), target);
        }
        throw new ConversionException("No from-string converter for " + target.getSimpleName());
    }

    private Codec<?> lookupVt(Class<?> clazz, ValueType vt) {
        Map<ValueType, Codec<?>> perVt = vtCodecs.get(clazz);
        if (perVt != null) {
            Codec<?> c = perVt.get(vt);
            if (c != null) return c;
        }
        if (getParent() instanceof JavaTypeRegistry p) {
            return p.lookupVt(clazz, vt);
        }
        return null;
    }

    /// Converts a SpeedyValue to a Java object of the target class.
    /// Uses the codec's {@link Codec#safeEncode(SpeedyValue)} for verified conversion.
    public <T> T toJava(SpeedyValue sv, Class<T> targetClass) throws SpeedyHttpException {
        if (sv == null || sv instanceof SpeedyNull) {
            return null;
        }
        Class<?> wrapped = wrap(targetClass);

        ValueType vt = sv.getValueType();
        Codec<?> vtCodec = lookupVt(wrapped, vt);
        if (vtCodec != null) {
            return checkedCast(vtCodec.safeEncode(sv), targetClass);
        }

        Codec<?> codec = lookup(wrapped);
        if (codec != null) {
            return checkedCast(codec.safeEncode(sv), targetClass);
        }

        if (wrapped.isEnum()) {
            return checkedCast(convertEnumFromSpeedy(sv, wrapped), targetClass);
        }

        throw new ConversionException("No converter found for " + vt + " -> " + wrapped.getName());
    }

    /// Converts a Java object to a SpeedyValue.
    /// Uses the codec's {@link Codec#safeDecode(Object)} for verified conversion.
    public SpeedyValue toSpeedy(Object instance, ValueType vt) throws SpeedyHttpException {
        if (instance == null) {
            return SpeedyNull.SPEEDY_NULL;
        }
        Class<?> clazz = wrap(instance.getClass());

        Codec<?> vtCodec = lookupVt(clazz, vt);
        if (vtCodec != null) {
            return vtCodec.safeDecode(instance);
        }

        Codec<?> codec = lookup(clazz);
        if (codec != null) {
            return codec.safeDecode(instance);
        }

        if (clazz.isEnum()) {
            return convertEnumToSpeedy((Enum<?>) instance, vt);
        }

        throw new ConversionException("No converter found for " + clazz.getName() + " -> " + vt);
    }

    public boolean canToJava(ValueType vt, Class<?> javaType) {
        Class<?> wrapped = wrap(javaType);
        return lookupVt(wrapped, vt) != null || lookup(wrapped) != null || wrapped.isEnum();
    }

    public boolean canToSpeedy(ValueType vt, Class<?> javaType) {
        Class<?> wrapped = wrap(javaType);
        return lookupVt(wrapped, vt) != null || lookup(wrapped) != null || wrapped.isEnum();
    }
}
