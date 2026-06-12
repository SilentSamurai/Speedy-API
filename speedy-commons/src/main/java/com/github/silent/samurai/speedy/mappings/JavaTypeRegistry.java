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
import java.util.*;
import java.util.function.Function;

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

    private final Map<Class<?>, Map<ValueType, Codec>> vtCodecs = new HashMap<>();

    public JavaTypeRegistry(JavaTypeRegistry parent) {
        super(parent);
    }

    public JavaTypeRegistry register(Class<?> clazz, ValueType vt,
                                      Function<SpeedyValue, Object> encode,
                                      Function<Object, SpeedyValue> decode) {
        vtCodecs.computeIfAbsent(clazz, k -> new HashMap<>()).put(vt, new Codec(encode, decode));
        return this;
    }

    private Codec lookupVt(Class<?> clazz, ValueType vt) {
        Map<ValueType, Codec> perVt = vtCodecs.get(clazz);
        if (perVt != null) {
            Codec c = perVt.get(vt);
            if (c != null) return c;
        }
        if (getParent() instanceof JavaTypeRegistry p) {
            return p.lookupVt(clazz, vt);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T toJava(SpeedyValue sv, Class<T> targetClass) throws SpeedyHttpException {
        if (sv == null || sv instanceof SpeedyNull) {
            return null;
        }
        Class<?> wrapped = wrap(targetClass);

        ValueType vt = sv.getValueType();
        Codec vtCodec = lookupVt(wrapped, vt);
        if (vtCodec != null) {
            return (T) vtCodec.encode().apply(sv);
        }

        Codec codec = lookup(wrapped);
        if (codec != null) {
            return (T) codec.encode().apply(sv);
        }

        if (wrapped.isEnum()) {
            return (T) convertEnumFromSpeedy(sv, (Class<? extends Enum>) wrapped);
        }

        throw new ConversionException("No converter found for " + vt + " -> " + wrapped.getName());
    }

    @SuppressWarnings("unchecked")
    public SpeedyValue toSpeedy(Object instance, ValueType vt) throws SpeedyHttpException {
        if (instance == null) {
            return SpeedyNull.SPEEDY_NULL;
        }
        Class<?> clazz = wrap(instance.getClass());

        Codec vtCodec = lookupVt(clazz, vt);
        if (vtCodec != null) {
            return vtCodec.decode().apply(instance);
        }

        Codec codec = lookup(clazz);
        if (codec != null) {
            return codec.decode().apply(instance);
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
            default ->
                    throw new ConversionException("Cannot convert enum " + en.getClass().getSimpleName() + " to " + vt);
        };
    }

    @SuppressWarnings("unchecked")
    public static JavaTypeRegistry defaults() {
        JavaTypeRegistry r = new JavaTypeRegistry(null);

        r.register(String.class,
                sv -> ((SpeedyText) sv).getValue(),
                raw -> new SpeedyText((String) raw));

        r.register(UUID.class,
                sv -> UUID.fromString(sv.asText()),
                uuid -> new SpeedyText(((UUID) uuid).toString()));

        r.register(Boolean.class,
                sv -> ((SpeedyBoolean) sv).getValue(),
                raw -> new SpeedyBoolean((Boolean) raw));

        r.register(Long.class,
                sv -> ((SpeedyInt) sv).getValue(),
                raw -> new SpeedyInt((Long) raw));

        r.register(Integer.class,
                sv -> ((SpeedyInt) sv).getValue().intValue(),
                val -> new SpeedyInt(((Integer) val).longValue()));

        r.register(BigInteger.class,
                sv -> BigInteger.valueOf(((SpeedyInt) sv).getValue()),
                bi -> new SpeedyInt(((BigInteger) bi).longValue()));

        r.register(Double.class,
                sv -> ((SpeedyDouble) sv).getValue(),
                raw -> new SpeedyDouble((Double) raw));

        r.register(Float.class,
                sv -> ((SpeedyDouble) sv).getValue().floatValue(),
                f -> new SpeedyDouble(((Float) f).doubleValue()));

        r.register(BigDecimal.class,
                sv -> BigDecimal.valueOf(((SpeedyDouble) sv).getValue()),
                bd -> new SpeedyDouble(((BigDecimal) bd).doubleValue()));

        r.register(BigDecimal.class, ValueType.FLOAT,
                sv -> BigDecimal.valueOf(((SpeedyDouble) sv).getValue()),
                bd -> new SpeedyDouble(((BigDecimal) bd).doubleValue()));

        r.register(BigDecimal.class, ValueType.INT,
                sv -> BigDecimal.valueOf(((SpeedyInt) sv).getValue().longValue()),
                bd -> new SpeedyInt(((BigDecimal) bd).longValue()));

        r.register(BigInteger.class, ValueType.FLOAT,
                sv -> BigInteger.valueOf(((SpeedyDouble) sv).getValue().longValue()),
                bi -> new SpeedyDouble(((BigInteger) bi).doubleValue()));

        r.register(LocalDate.class,
                sv -> ((SpeedyDate) sv).asDate(),
                raw -> new SpeedyDate((LocalDate) raw));

        r.register(java.util.Date.class,
                sv -> {
                    Instant instant = ((SpeedyDate) sv).getValue().atStartOfDay(ZoneId.of("UTC")).toInstant();
                    return java.util.Date.from(instant);
                },
                d -> new SpeedyDate(LocalDate.ofInstant(((java.util.Date) d).toInstant(), ZoneId.of("UTC"))));

        r.register(java.sql.Date.class,
                sv -> java.sql.Date.valueOf(((SpeedyDate) sv).getValue()),
                sql -> new SpeedyDate(((java.sql.Date) sql).toLocalDate()));

        r.register(LocalTime.class,
                sv -> ((SpeedyTime) sv).asTime(),
                raw -> new SpeedyTime((LocalTime) raw));

        r.register(LocalDateTime.class,
                sv -> ((SpeedyDateTime) sv).getValue(),
                raw -> new SpeedyDateTime((LocalDateTime) raw));

        r.register(Instant.class, ValueType.DATE_TIME,
                sv -> ((SpeedyDateTime) sv).getValue().atZone(ZoneId.of("UTC")).toInstant(),
                instant -> new SpeedyDateTime(LocalDateTime.ofInstant((Instant) instant, ZoneId.of("UTC"))));

        r.register(Instant.class, ValueType.ZONED_DATE_TIME,
                sv -> ((SpeedyZonedDateTime) sv).asZonedDateTime().toInstant(),
                instant -> new SpeedyZonedDateTime(((Instant) instant).atZone(ZoneId.of("UTC"))));

        r.register(Timestamp.class,
                sv -> Timestamp.valueOf(((SpeedyDateTime) sv).getValue()),
                ts -> new SpeedyDateTime(((Timestamp) ts).toLocalDateTime()));

        r.register(ZonedDateTime.class,
                sv -> ((SpeedyZonedDateTime) sv).asZonedDateTime(),
                raw -> new SpeedyZonedDateTime((ZonedDateTime) raw));

        return r;
    }
}
