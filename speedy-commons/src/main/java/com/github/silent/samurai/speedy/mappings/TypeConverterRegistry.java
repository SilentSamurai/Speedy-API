package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A central, bidirectional registry that stores converters between
 *  - scalar {@link SpeedyValue} instances and ordinary Java types (TO_JAVA / TO_SPEEDY)
 *  - raw {@link String} literals and Java primitives (FROM_STRING)
 *
 * It replaces the duplicated static converter maps that used to live in
 * {@link SpeedySerializer} and {@link SpeedyDeserializer}.
 *
 * For the first iteration, the registry will *delegate* to those legacy classes
 * when a converter has not yet been registered here. This guarantees
 * backward-compatibility while we migrate definitions gradually.
 */
public final class TypeConverterRegistry {

    /** Direction of transformation */
    public enum Direction {
        TO_JAVA,      // SpeedyValue  -> Java
        TO_SPEEDY,    // Java         -> SpeedyValue
        FROM_STRING   // raw String   -> Java
    }

    /** Small functional interface that allows throwing {@link SpeedyHttpException}. */
    @FunctionalInterface
    public interface Converter<S, T> {
        T apply(S source) throws SpeedyHttpException;
    }

    private record ConverterKey(Class<?> javaType, ValueType speedyType, Direction direction) {
        // Auto-generated equals / hashCode / toString by record.
    }

    // Thread-safe map so the registry can be extended at runtime (e.g. by users)
    private static final Map<ConverterKey, Converter<?, ?>> REGISTRY = new ConcurrentHashMap<>();

    // Mapping from primitive classes to their wrapper counterparts to deduplicate converter entries
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

    static {
        // Load the first batch of default converters so new code paths can use the registry
        initDefaultConverters();
    }

    private TypeConverterRegistry() {
    }

    /* --------------------------------------------------
       Registration helpers
       -------------------------------------------------- */

    public static <J> void register(ValueType valueType,
                                    Class<J> javaType,
                                    Converter<SpeedyValue, J> toJava,
                                    Converter<J, SpeedyValue> toSpeedy) {
        register(valueType, javaType, Direction.TO_JAVA, toJava);
        register(valueType, javaType, Direction.TO_SPEEDY, toSpeedy);
    }

    public static <S, T> void register(ValueType valueType,
                                       Class<?> javaType,
                                       Direction direction,
                                       Converter<S, T> fn) {
        REGISTRY.put(new ConverterKey(javaType, valueType, direction), fn);
    }

    /* --------------------------------------------------
       Lookup helpers – public API
       -------------------------------------------------- */

    public static <T> T toJava(SpeedyValue speedyValue, Class<T> targetClass) throws SpeedyHttpException {
        if (speedyValue == null || speedyValue instanceof SpeedyNull) {
            return null;
        }
        ValueType vt = speedyValue.getValueType();
        Class<?> wrapped = wrap(targetClass);

        // First try registry
        @SuppressWarnings("unchecked")
        Converter<SpeedyValue, ?> conv = (Converter<SpeedyValue, ?>) getConverter(vt, wrapped, Direction.TO_JAVA);
        if (conv != null) {
            //noinspection unchecked
            return (T) conv.apply(speedyValue);
        }

        // Enum special-case fallback
        if (wrapped.isEnum()) {
            // Safe: wrapped.isEnum() ensures this cast is valid at runtime
            return (T) convertEnumFromSpeedy(speedyValue, (Class<? extends Enum>) wrapped);
        }

        throw new ConversionException("No converter found for " + vt + " -> " + wrapped.getName());
    }

    public static SpeedyValue toSpeedy(Object instance, ValueType vt) throws SpeedyHttpException {
        if (instance == null) {
            return SpeedyNull.SPEEDY_NULL;
        }
        Class<?> clazz = wrap(instance.getClass());

        // Registry first
        Converter<Object, ?> conv = (Converter<Object, ?>) getConverter(vt, clazz, Direction.TO_SPEEDY);
        if (conv != null) {
            return (SpeedyValue) conv.apply(instance);
        }

        // Enum fallback
        if (clazz.isEnum()) {
            return convertEnumToSpeedy((Enum<?>) instance, vt);
        }

        throw new ConversionException("No converter found for " + clazz.getName() + " -> " + vt);
    }

    /**
     * Converts a raw String literal (usually from query parameters) into a Java primitive / wrapper value.
     */
    public static <T> T fromString(String literal, Class<T> target) throws SpeedyHttpException {
        Class<?> wrapped = wrap(target);

        Converter<String, ?> conv = (Converter<String, ?>) getConverter(null, wrapped, Direction.FROM_STRING);
        if (conv != null) {
            //noinspection unchecked
            return (T) conv.apply(literal);
        }

        throw new ConversionException("No converter found for literal '" + literal + "' to " + target.getSimpleName());
    }

    /* --------------------------------------------------
       Internal helpers
       -------------------------------------------------- */

    private static Converter<?, ?> getConverter(ValueType vt, Class<?> javaType, Direction dir) {
        // Direct key
        Converter<?, ?> conv = REGISTRY.get(new ConverterKey(javaType, vt, dir));
        if (conv != null) {
            return conv;
        }
        // If javaType is primitive, try wrapper or vice-versa
        Class<?> alt = primitiveWrapperAlternate(javaType);
        if (alt != null) {
            conv = REGISTRY.get(new ConverterKey(alt, vt, dir));
        }
        return conv;
    }

    private static Class<?> primitiveWrapperAlternate(Class<?> c) {
        if (c.isPrimitive()) {
            return PRIMITIVE_TO_WRAPPER.get(c);
        }
        // reverse lookup
        Optional<? extends Class<?>> primitive = PRIMITIVE_TO_WRAPPER.entrySet().stream()
                .filter(e -> e.getValue() == c)
                .map(Map.Entry::getKey)
                .findFirst();
        return primitive.orElse(null);
    }

    private static Class<?> wrap(Class<?> clazz) {
        return clazz.isPrimitive() ? PRIMITIVE_TO_WRAPPER.getOrDefault(clazz, clazz) : clazz;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends Enum<E>> E convertEnumFromSpeedy(SpeedyValue sv, Class<? extends Enum> enumClass) {
        if (sv.isText()) {
            return (E) Enum.valueOf((Class) enumClass, sv.asText());
        }
        if (sv.isEnum()) {
            return (E) Enum.valueOf((Class) enumClass, sv.asEnum());
        }
        if (sv.isEnumOrd()) {
            int ord = sv.asEnumOrd().intValue();
            Enum[] constants = (Enum[]) enumClass.getEnumConstants();
            if (constants != null && ord >= 0 && ord < constants.length) {
                return (E) constants[ord];
            }
        }
        throw new ConversionException("Cannot convert " + sv + " to enum " + enumClass.getSimpleName());
    }

    private static SpeedyValue convertEnumToSpeedy(Enum<?> en, ValueType vt) {
        return switch (vt) {
            case TEXT, ENUM -> new SpeedyText(en.name());
            case INT, ENUM_ORD -> new SpeedyInt((long) en.ordinal());
            default -> throw new ConversionException("Cannot convert enum " + en.getClass().getSimpleName() + " to " + vt);
        };
    }

    /* --------------------------------------------------
       Capability helpers for callers that need to check conversion availability
       -------------------------------------------------- */

    public static boolean canToJava(ValueType vt, Class<?> javaType) {
        return getConverter(vt, wrap(javaType), Direction.TO_JAVA) != null;
    }

    public static boolean canToSpeedy(ValueType vt, Class<?> javaType) {
        return getConverter(vt, wrap(javaType), Direction.TO_SPEEDY) != null;
    }

    /* --------------------------------------------------
       Placeholder for future – load defaults into registry to decouple from legacy classes
       -------------------------------------------------- */

    private static void initDefaultConverters() {
        /* ---------------- TEXT ---------------- */
        register(ValueType.TEXT, String.class,
                sv -> ((SpeedyText) sv).getValue(),
                SpeedyText::new);

        register(ValueType.TEXT, UUID.class,
                sv -> UUID.fromString(sv.asText()),
                uuid -> new SpeedyText(uuid.toString()));

        /* ---------------- BOOL ---------------- */
        register(ValueType.BOOL, Boolean.class,
                sv -> ((SpeedyBoolean) sv).getValue(),
                SpeedyBoolean::new);

        /* ---------------- INT ---------------- */
        register(ValueType.INT, Long.class,
                sv -> ((SpeedyInt) sv).getValue(),
                SpeedyInt::new);

        register(ValueType.INT, Integer.class,
                sv -> ((SpeedyInt) sv).getValue().intValue(),
                val -> new SpeedyInt(val.longValue()));

        register(ValueType.INT, BigInteger.class,
                sv -> BigInteger.valueOf(((SpeedyInt) sv).getValue()),
                bi -> new SpeedyInt(bi.longValue()));

        register(ValueType.INT, BigDecimal.class,
                sv -> BigDecimal.valueOf(((SpeedyInt) sv).getValue()),
                bd -> new SpeedyInt(bd.longValue()));

        /* ---------------- FLOAT ---------------- */
        register(ValueType.FLOAT, Double.class,
                sv -> ((SpeedyDouble) sv).getValue(),
                SpeedyDouble::new);

        register(ValueType.FLOAT, Float.class,
                sv -> ((SpeedyDouble) sv).getValue().floatValue(),
                f -> new SpeedyDouble(Double.valueOf(f)));

        register(ValueType.FLOAT, BigDecimal.class,
                sv -> BigDecimal.valueOf(((SpeedyDouble) sv).getValue()),
                bd -> new SpeedyDouble(bd.doubleValue()));

        register(ValueType.FLOAT, BigInteger.class,
                sv -> BigInteger.valueOf(((SpeedyDouble) sv).getValue().longValue()),
                bi -> new SpeedyDouble(bi.doubleValue()));

        /* ---------------- DATE ---------------- */
        register(ValueType.DATE, LocalDate.class,
                sv -> ((SpeedyDate) sv).asDate(),
                SpeedyDate::new);

        register(ValueType.DATE, java.util.Date.class,
                sv -> {
                    Instant instant = ((SpeedyDate) sv).getValue().atStartOfDay(ZoneId.of("UTC")).toInstant();
                    return java.util.Date.from(instant);
                },
                d -> new SpeedyDate(LocalDate.ofInstant(d.toInstant(), ZoneId.of("UTC"))));

        register(ValueType.DATE, java.sql.Date.class,
                sv -> java.sql.Date.valueOf(((SpeedyDate) sv).getValue()),
                sql -> new SpeedyDate(sql.toLocalDate()));

        /* ---------------- TIME ---------------- */
        register(ValueType.TIME, LocalTime.class,
                sv -> ((SpeedyTime) sv).asTime(),
                SpeedyTime::new);

        /* ---------------- DATE_TIME ---------------- */
        register(ValueType.DATE_TIME, LocalDateTime.class,
                sv -> ((SpeedyDateTime) sv).getValue(),
                SpeedyDateTime::new);

        register(ValueType.DATE_TIME, Instant.class,
                sv -> ((SpeedyDateTime) sv).getValue().atZone(ZoneId.of("UTC")).toInstant(),
                instant -> new SpeedyDateTime(LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))));

        register(ValueType.DATE_TIME, Timestamp.class,
                sv -> Timestamp.valueOf(((SpeedyDateTime) sv).getValue()),
                ts -> new SpeedyDateTime(ts.toLocalDateTime()));

        /* ---------------- ZONED_DATE_TIME ---------------- */
        register(ValueType.ZONED_DATE_TIME, ZonedDateTime.class,
                sv -> ((SpeedyZonedDateTime) sv).asZonedDateTime(),
                SpeedyZonedDateTime::new);

        register(ValueType.ZONED_DATE_TIME, Instant.class,
                sv -> ((SpeedyZonedDateTime) sv).asZonedDateTime().toInstant(),
                instant -> new SpeedyZonedDateTime(instant.atZone(ZoneId.of("UTC"))));

        /* ---------------- FROM_STRING Converters ---------------- */
        register(null, Integer.class, Direction.FROM_STRING, (String v) -> Integer.parseInt(v));
        register(null, int.class, Direction.FROM_STRING, (String v) -> Integer.parseInt(v));
        register(null, Long.class, Direction.FROM_STRING, (String v) -> Long.parseLong(v));
        register(null, long.class, Direction.FROM_STRING, (String v) -> Long.parseLong(v));
        register(null, Float.class, Direction.FROM_STRING, (String v) -> Float.parseFloat(v));
        register(null, float.class, Direction.FROM_STRING, (String v) -> Float.parseFloat(v));
        register(null, Double.class, Direction.FROM_STRING, (String v) -> Double.parseDouble(v));
        register(null, double.class, Direction.FROM_STRING, (String v) -> Double.parseDouble(v));
        register(null, Boolean.class, Direction.FROM_STRING, (String v) -> Boolean.parseBoolean(v));
        register(null, boolean.class, Direction.FROM_STRING, (String v) -> Boolean.parseBoolean(v));
        register(null, String.class, Direction.FROM_STRING, (String v) -> v);
        register(null, UUID.class, Direction.FROM_STRING, (String v) -> UUID.fromString(v));
        register(null, LocalDate.class, Direction.FROM_STRING, (String v) -> LocalDate.parse(v));
        register(null, LocalDateTime.class, Direction.FROM_STRING, (String v) -> LocalDateTime.parse(v));
        register(null, LocalTime.class, Direction.FROM_STRING, (String v) -> LocalTime.parse(v));
        register(null, ZonedDateTime.class, Direction.FROM_STRING, (String v) -> ZonedDateTime.parse(v));
        register(null, Instant.class, Direction.FROM_STRING, (String v) -> Instant.parse(v));
    }
}
