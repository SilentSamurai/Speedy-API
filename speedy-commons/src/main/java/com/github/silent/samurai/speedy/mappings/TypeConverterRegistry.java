package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.time.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TypeConverterRegistry {

    private static final JavaTypeRegistry DEFAULT = JavaTypeRegistry.defaults();

    private static final Map<ConverterKey, Converter<?, ?>> FROM_STRING_REGISTRY = new ConcurrentHashMap<>();

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
        initFromStringConverters();
    }

    private TypeConverterRegistry() {
    }

    public static <J> void register(ValueType valueType,
                                    Class<J> javaType,
                                    Converter<SpeedyValue, J> toJava,
                                    Converter<J, SpeedyValue> toSpeedy) {
        DEFAULT.register(javaType, valueType,
                sv -> {
                    try {
                        return toJava.apply(sv);
                    } catch (SpeedyHttpException e) {
                        throw new RuntimeException(e);
                    }
                },
                raw -> {
                    try {
                        return toSpeedy.apply((J) raw);
                    } catch (SpeedyHttpException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static <T> T toJava(SpeedyValue speedyValue, Class<T> targetClass) throws SpeedyHttpException {
        return DEFAULT.toJava(speedyValue, targetClass);
    }

    public static SpeedyValue toSpeedy(Object instance, ValueType vt) throws SpeedyHttpException {
        return DEFAULT.toSpeedy(instance, vt);
    }

    public static <T> T fromString(String literal, Class<T> target) throws SpeedyHttpException {
        Class<?> wrapped = wrap(target);

        Converter<String, ?> conv = (Converter<String, ?>) getFromStringConverter(wrapped);
        if (conv != null) {
            return (T) conv.apply(literal);
        }

        throw new ConversionException("No converter found for literal '" + literal + "' to " + target.getSimpleName());
    }

    private static Converter<?, ?> getFromStringConverter(Class<?> javaType) {
        Converter<?, ?> conv = FROM_STRING_REGISTRY.get(new ConverterKey(javaType, null, Direction.FROM_STRING));
        if (conv != null) return conv;
        Class<?> alt = primitiveWrapperAlternate(javaType);
        if (alt != null) {
            conv = FROM_STRING_REGISTRY.get(new ConverterKey(alt, null, Direction.FROM_STRING));
        }
        return conv;
    }

    private static Class<?> primitiveWrapperAlternate(Class<?> c) {
        if (c.isPrimitive()) {
            return PRIMITIVE_TO_WRAPPER.get(c);
        }
        Optional<? extends Class<?>> primitive = PRIMITIVE_TO_WRAPPER.entrySet().stream()
                .filter(e -> e.getValue() == c)
                .map(Map.Entry::getKey)
                .findFirst();
        return primitive.orElse(null);
    }

    private static Class<?> wrap(Class<?> clazz) {
        return clazz.isPrimitive() ? PRIMITIVE_TO_WRAPPER.getOrDefault(clazz, clazz) : clazz;
    }

    public static boolean canToJava(ValueType vt, Class<?> javaType) {
        return DEFAULT.canToJava(vt, javaType);
    }

    public static boolean canToSpeedy(ValueType vt, Class<?> javaType) {
        return DEFAULT.canToSpeedy(vt, javaType);
    }

    private static void initFromStringConverters() {
        fromStringReg(Integer.class, v -> Integer.parseInt(v));
        fromStringReg(int.class, v -> Integer.parseInt(v));
        fromStringReg(Long.class, v -> Long.parseLong(v));
        fromStringReg(long.class, v -> Long.parseLong(v));
        fromStringReg(Float.class, v -> Float.parseFloat(v));
        fromStringReg(float.class, v -> Float.parseFloat(v));
        fromStringReg(Double.class, v -> Double.parseDouble(v));
        fromStringReg(double.class, v -> Double.parseDouble(v));
        fromStringReg(Boolean.class, v -> Boolean.parseBoolean(v));
        fromStringReg(boolean.class, v -> Boolean.parseBoolean(v));
        fromStringReg(String.class, v -> v);
        fromStringReg(UUID.class, v -> UUID.fromString(v));
        fromStringReg(LocalDate.class, v -> LocalDate.parse(v));
        fromStringReg(LocalDateTime.class, v -> LocalDateTime.parse(v));
        fromStringReg(LocalTime.class, v -> LocalTime.parse(v));
        fromStringReg(ZonedDateTime.class, v -> ZonedDateTime.parse(v));
        fromStringReg(Instant.class, v -> Instant.parse(v));
    }

    private static <T> void fromStringReg(Class<T> clazz, Converter<String, T> fn) {
        FROM_STRING_REGISTRY.put(new ConverterKey(clazz, null, Direction.FROM_STRING), fn);
        FROM_STRING_REGISTRY.put(new ConverterKey(wrap(clazz), null, Direction.FROM_STRING), fn);
    }

    public enum Direction {
        TO_JAVA,
        TO_SPEEDY,
        FROM_STRING
    }

    @FunctionalInterface
    public interface Converter<S, T> {
        T apply(S source) throws SpeedyHttpException;
    }

    private record ConverterKey(Class<?> javaType, ValueType speedyType, Direction direction) {
    }
}
