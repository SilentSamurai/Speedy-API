package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.ThrowingBiFunction;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpeedySerializer {

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

    private static <T> T toJavaField(SpeedyValue value, FieldMetadata fieldMetadata, Class<T> clazz) throws SpeedyHttpException {
        ValueType valueType = value.getValueType();
        // handle null
        if (value instanceof SpeedyNull) {
            return null;
        }

        // Handle enums
        if (!has(valueType, clazz)) {
            if (clazz.isEnum()) {
                // Safe because convertToEnum returns the right enum subtype
                return (T) CommonUtil.convertToEnum((Class<? extends Enum>) clazz, value);
            }
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

    public static <T> T asJavaObject(SpeedyValue speedyValue) {
        return switch (speedyValue.getValueType()) {
            case NULL:
                yield null;
            case BOOL:
                if (speedyValue.isBoolean()) {
                    yield (T) speedyValue.asBoolean();
                }
            case TEXT:
                if (speedyValue.isText()) {
                    yield (T) speedyValue.asText();
                }
            case ENUM:
                if (speedyValue.isEnum()) {
                    yield (T) speedyValue.asEnum();
                }
            case ENUM_ORD:
                if (speedyValue.isEnumOrd()) {
                    yield (T) speedyValue.asEnumOrd();
                }
            case INT:
                if (speedyValue.isInt()) {
                    yield (T) speedyValue.asInt();
                }
            case FLOAT:
                if (speedyValue.isDouble()) {
                    yield (T) speedyValue.asDouble();
                }
            case DATE:
                if (speedyValue.isDate()) {
                    yield (T) speedyValue.asDate();
                }
            case TIME:
                if (speedyValue.isTime()) {
                    yield (T) speedyValue.asTime();
                }
            case DATE_TIME:
                if (speedyValue.isDateTime()) {
                    yield (T) speedyValue.asDateTime();
                }
            case ZONED_DATE_TIME:
                if (speedyValue.isZonedDateTime()) {
                    yield (T) speedyValue.asZonedDateTime();
                }
            case OBJECT:
            case COLLECTION:
                throw new ConversionException(
                        String.format("Cannot convert %s to %s", speedyValue, speedyValue.getValueType().name())
                );
        };
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
    public static <T> T toJavaObject(SpeedyEntity value, Class<T> clazz) throws SpeedyHttpException {
        if (value == null) {
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

            Map<String, Field> fieldMap = Arrays.stream(clazz.getDeclaredFields())
                    .collect(Collectors.toMap(Field::getName, Function.identity()));

            // Loop through only the fields known to Speedy (fields in entity metadata)
            for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
                String fieldName = fieldMetadata.getOutputPropertyName();
                // Check if the entity has the value for this field
                if (!entity.has(fieldMetadata)) {
                    continue;
                }
                // Check if the class has the field
                if (!fieldMap.containsKey(fieldName)) {
                    continue;
                }

                Field targetField = fieldMap.get(fieldName);

                try {
                    // Get the value from the entity
                    SpeedyValue fieldValue = entity.get(fieldMetadata);
                    // Associations: if this field represents an association and
                    // the value is an object, recursively convert it to the
                    // target type. Associated SpeedyEntity may contain only key fields.
                    Object convertedValue;
                    if (fieldValue instanceof SpeedyNull) {
                        convertedValue = null;
                    } else if (fieldMetadata.isAssociation() && fieldValue.isObject()) {
                        convertedValue = toJavaObject(fieldValue.asObject(), targetField.getType());
                    } else {
                        // Scalar or directly convertible values
                        convertedValue = toJavaField(fieldValue, fieldMetadata, targetField.getType());
                    }

                    BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(instance);
                    if (wrapper.isWritableProperty(fieldName)) {
                        wrapper.setPropertyValue(fieldName, convertedValue);
                    }
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
