package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.ThrowingBiFunction;
import com.github.silent.samurai.speedy.models.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeedyValue2JavaType {

    private static final Map<String, ThrowingBiFunction<SpeedyValue, ValueType, Object, SpeedyHttpException>> converters = new HashMap<>();

    static {
        initConverters();
    }

    public static <T> ThrowingBiFunction<SpeedyValue, ValueType, Object, SpeedyHttpException> get(ValueType valueType, Class<T> clazz) {
        return converters.get(getKey(valueType, clazz));
    }

    public static boolean has(ValueType valueType, Class<?> clazz) {
        return converters.containsKey(getKey(valueType, clazz));
    }

    private static String getKey(ValueType valueType, Class<?> clazz) {
        return valueType.name() + "_" + clazz.getName();
    }

    public static <T> void put(ValueType valueType, Class<T> clazz,
                               ThrowingBiFunction<SpeedyValue, ValueType, T, SpeedyHttpException> converter) {
        converters.put(getKey(valueType, clazz),
                (ThrowingBiFunction<SpeedyValue, ValueType, Object, SpeedyHttpException>) converter);
    }

    private static Class<?> getWrapperClass(Class<?> clazz) {
        if (clazz == int.class) return Integer.class;
        if (clazz == long.class) return Long.class;
        if (clazz == double.class) return Double.class;
        if (clazz == float.class) return Float.class;
        if (clazz == boolean.class) return Boolean.class;
        if (clazz == byte.class) return Byte.class;
        if (clazz == char.class) return Character.class;
        if (clazz == short.class) return Short.class;
        if (clazz == void.class) return Void.class;
        return clazz; // Return the same class if it's not a primitive
    }

    private static void initConverters() {

        put(ValueType.TEXT, String.class, (speedyValue, type) -> {
            return ((SpeedyText) speedyValue).getValue();
        });
        put(ValueType.TEXT, UUID.class, (speedyValue, type) -> {
            return UUID.fromString(speedyValue.asText());
        });
        put(ValueType.INT, int.class, (speedyValue, type) -> {
            SpeedyInt speedyInt = (SpeedyInt) speedyValue;
            return speedyInt.getValue().intValue();
        });
        put(ValueType.INT, Integer.class, (speedyValue, type) -> {
            SpeedyInt speedyInt = (SpeedyInt) speedyValue;
            return speedyInt.getValue().intValue();
        });

        put(ValueType.INT, long.class, (speedyValue, type) -> {
            SpeedyInt speedyInt = (SpeedyInt) speedyValue;
            return speedyInt.getValue();
        });
        put(ValueType.INT, Long.class, (speedyValue, type) -> {
            SpeedyInt speedyInt = (SpeedyInt) speedyValue;
            return speedyInt.getValue();
        });
        put(ValueType.INT, BigInteger.class, (speedyValue, type) -> {
            SpeedyInt speedyInt = (SpeedyInt) speedyValue;
            return BigInteger.valueOf(speedyInt.getValue());
        });
        put(ValueType.INT, BigDecimal.class, (speedyValue, type) -> {
            SpeedyInt speedyInt = (SpeedyInt) speedyValue;
            return BigDecimal.valueOf(speedyInt.getValue());
        });
        put(ValueType.FLOAT, float.class, (speedyValue, type) -> {
            SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
            return speedyDouble.getValue().floatValue();
        });
        put(ValueType.FLOAT, Float.class, (speedyValue, type) -> {
            SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
            return speedyDouble.getValue().floatValue();
        });
        put(ValueType.FLOAT, double.class, (speedyValue, type) -> {
            SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
            return speedyDouble.getValue();
        });
        put(ValueType.FLOAT, Double.class, (speedyValue, type) -> {
            SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
            return speedyDouble.getValue();
        });
        put(ValueType.FLOAT, BigDecimal.class, (speedyValue, type) -> {
            SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
            return BigDecimal.valueOf(speedyDouble.getValue());
        });
        put(ValueType.FLOAT, BigInteger.class, (speedyValue, type) -> {
            SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
            return BigInteger.valueOf(speedyDouble.getValue().longValue());
        });
        put(ValueType.BOOL, boolean.class, (speedyValue, type) -> {
            SpeedyBoolean speedyBoolean = (SpeedyBoolean) speedyValue;
            return speedyBoolean.getValue();
        });
        put(ValueType.BOOL, Boolean.class, (speedyValue, type) -> {
            SpeedyBoolean speedyBoolean = (SpeedyBoolean) speedyValue;
            return speedyBoolean.getValue();
        });
        put(ValueType.DATE, java.sql.Date.class, (speedyValue, type) -> {
            SpeedyDate speedyDate = (SpeedyDate) speedyValue;
            return java.sql.Date.valueOf(speedyDate.getValue());
        });
        put(ValueType.DATE, Date.class, (speedyValue, type) -> {
            SpeedyDate speedyDate = (SpeedyDate) speedyValue;
            Instant instant = speedyDate.getValue().atStartOfDay(ZoneId.of("UTC")).toInstant();
            return Date.from(instant);
        });
        put(ValueType.DATE, LocalDate.class, (speedyValue, type) -> {
            SpeedyDate speedyDate = (SpeedyDate) speedyValue;
            return speedyDate.asDate();
        });
        put(ValueType.TIME, LocalTime.class, (speedyValue, type) -> {
            SpeedyTime speedyTime = (SpeedyTime) speedyValue;
            return speedyTime.asTime();
        });
//        put(ValueType.TIME, Instant.class, (speedyValue, type) -> {
//            SpeedyTime speedyTime = (SpeedyTime) speedyValue;
//            // convert localTime to Instant object
//            return speedyTime.getValue();
//        });
        put(ValueType.DATE_TIME, Instant.class, (speedyValue, type) -> {
            SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
            return speedyDateTime.getValue().atZone(ZoneId.of("UTC")).toInstant();
        });
        put(ValueType.DATE_TIME, LocalDate.class, (speedyValue, type) -> {
            SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
            return speedyDateTime.getValue().toLocalDate();
        });
        put(ValueType.DATE_TIME, LocalDateTime.class, (speedyValue, type) -> {
            SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
            return speedyDateTime.getValue();
        });
        put(ValueType.DATE_TIME, Timestamp.class, (speedyValue, type) -> {
            SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
            return Timestamp.valueOf(speedyDateTime.getValue());
        });
        put(ValueType.ZONED_DATE_TIME, ZonedDateTime.class, (speedyValue, type) -> {
            SpeedyZonedDateTime speedyDateTime = (SpeedyZonedDateTime) speedyValue;
            return speedyDateTime.asZonedDateTime();
        });

        put(ValueType.ZONED_DATE_TIME, Instant.class, (speedyValue, type) -> {
            SpeedyZonedDateTime speedyDateTime = (SpeedyZonedDateTime) speedyValue;
            return speedyDateTime.asZonedDateTime().toInstant();
        });
    }

    public static <T> T convert(SpeedyValue value, Class<T> clazz) throws SpeedyHttpException {
        ValueType valueType = value.getValueType();
        // handle null
        if (!has(valueType, clazz) || value instanceof SpeedyNull) {
            return null;
        }

        // For primitive types, try to find a converter for their wrapper class
        if (clazz.isPrimitive()) {
            Class<?> wrapperClass = getWrapperClass(clazz);
            if (has(valueType, wrapperClass)) {
                return (T) get(valueType, wrapperClass).apply(value, valueType);
            }
        }

        if (has(valueType, clazz)) {
            return clazz.cast(get(valueType, clazz).apply(value, valueType));
        }

        // Fallback to direct casting if types are compatible
        try {
            return clazz.cast(value);
        } catch (ClassCastException e) {
            throw new ConversionException(
                    String.format("Cannot convert %s (type: %s) to class %s", value, valueType, clazz.getSimpleName()), e);
        }
    }

    public static <T> T convert(SpeedyValue speedyValue, ValueType valueType) {
        switch (valueType) {
            case NULL:
                return null;
            case BOOL:
                if (speedyValue.isBoolean()) {
                    return (T) speedyValue.asBoolean();
                }
            case TEXT:
                if (speedyValue.isText()) {
                    return (T) speedyValue.asText();
                }
            case INT:
                if (speedyValue.isInt()) {
                    return (T) speedyValue.asInt();
                }
            case FLOAT:
                if (speedyValue.isDouble()) {
                    return (T) speedyValue.asDouble();
                }
            case DATE:
                if (speedyValue.isDate()) {
                    return (T) speedyValue.asDate();
                }
            case TIME:
                if (speedyValue.isTime()) {
                    return (T) speedyValue.asTime();
                }
            case DATE_TIME:
                if (speedyValue.isDateTime()) {
                    return (T) speedyValue.asDateTime();
                }
            case ZONED_DATE_TIME:
                if (speedyValue.isZonedDateTime()) {
                    return (T) speedyValue.asZonedDateTime();
                }
            case OBJECT:
            case COLLECTION:
            default:
                throw new ConversionException(
                        String.format("Cannot convert %s to %s", speedyValue, valueType.name())
                );
        }
    }

    /**
     * Converts a SpeedyValue entity to a composite class by mapping fields.
     *
     * @param value The SpeedyValue to convert (must be an entity)
     * @param clazz The target composite class type
     * @param <T>   The generic type of the target class
     * @return The converted composite object or null if conversion is not possible
     * @throws SpeedyHttpException If conversion fails
     */
    public static <T> T convertToCompositeClass(SpeedyValue value, Class<T> clazz) throws SpeedyHttpException {
        if (value == null || value instanceof SpeedyNull) {
            return null;
        }

        // Check if the value is an entity
        if (!value.isObject()) {
            throw new ConversionException(
                    String.format("Cannot convert %s (type: %s) to composite class %s, value is not an entity",
                            value, value.getValueType(), clazz.getSimpleName()));
        }

        try {
            SpeedyEntity entity = value.asObject();
            EntityMetadata entityMetadata = entity.getMetadata();

            // Create instance of target class
            T instance = clazz.getDeclaredConstructor().newInstance();

            // Get all fields of the target class
            java.lang.reflect.Field[] targetFields = clazz.getDeclaredFields();

            // Loop through each field in the target class
            for (java.lang.reflect.Field targetField : targetFields) {
                String fieldName = targetField.getName();
                try {
                    // Check if the field exists in the entity
                    if (!entityMetadata.has(fieldName)) {
                        continue;
                    }
                    // Get the field metadata
                    FieldMetadata fieldMetadata = entityMetadata.field(fieldName);

                    // speedy entity does not have the value
                    if (!entity.has(fieldMetadata)) {
                        continue;
                    }

                    // Get the value from the entity
                    SpeedyValue fieldValue = entity.get(fieldMetadata);

                    // Convert the field value to the target field type
                    Object convertedValue = convert(fieldValue, targetField.getType());

                    // Set the field value in the target object
                    targetField.setAccessible(true);
                    targetField.set(instance, convertedValue);
                } catch (Exception e) {
                    // Skip the field if conversion fails
                    throw new ConversionException(
                            String.format("Failed to convert field %s in class %s: %s",
                                    fieldName, clazz.getSimpleName(), e.getMessage()), e);
                }
            }

            return instance;
        } catch (Exception e) {
            throw new ConversionException(
                    String.format("Cannot convert %s to composite class %s", value, clazz.getSimpleName()), e);
        }
    }


}
