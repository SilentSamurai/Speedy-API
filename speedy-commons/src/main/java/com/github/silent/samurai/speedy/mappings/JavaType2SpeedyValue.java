package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.ThrowingBiFunction;
import com.github.silent.samurai.speedy.models.*;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JavaType2SpeedyValue {
    private static final Map<String, ThrowingBiFunction<Object, ValueType, SpeedyValue, SpeedyHttpException>> converters = new HashMap<>();

    static {
        initConverters();
    }

    public static <T> ThrowingBiFunction<Object, ValueType, SpeedyValue, SpeedyHttpException> get(ValueType valueType, Class<T> clazz) {
        String key = clazz.getName() + valueType.name();
        return converters.get(key);
    }

    public static boolean has(ValueType valueType, Class<?> clazz) {
        String key = clazz.getName() + valueType.name();
        return converters.containsKey(key);
    }

    public static <T> void put(ValueType valueType, Class<T> clazz,
                               ThrowingBiFunction<T, ValueType, SpeedyValue, SpeedyHttpException> lambda) {
        String key = clazz.getName() + valueType.name();
        converters.put(key, (ThrowingBiFunction<Object, ValueType, SpeedyValue, SpeedyHttpException>) lambda);
    }

    public static <T> SpeedyValue convert(Class<T> clazz, ValueType valueType, Object instance) throws SpeedyHttpException {
        if (instance == null || !has(valueType, clazz)) {
            return SpeedyNull.SPEEDY_NULL;
        }
        return get(valueType, clazz).apply(instance, valueType);
    }

    private static void initConverters() {
        put(ValueType.TEXT, String.class, (instance, valueType) -> {
            return new SpeedyText((String) instance);
        });
        put(ValueType.TEXT, UUID.class, (instance, valueType) -> {
            return new SpeedyText(String.valueOf(instance));
        });
        put(ValueType.INT, int.class, (instance, valueType) -> {
            Integer value = (Integer) instance;
            return new SpeedyInt(value.longValue());
        });
        put(ValueType.INT, Integer.class, (instance, valueType) -> {
            Integer value = (Integer) instance;
            return new SpeedyInt(value.longValue());
        });
        put(ValueType.INT, long.class, (instance, valueType) -> {
            return new SpeedyInt((Long) instance);
        });
        put(ValueType.INT, Long.class, (instance, valueType) -> {
            return new SpeedyInt((Long) instance);
        });
        put(ValueType.INT, BigInteger.class, (instance, valueType) -> {
            return new SpeedyInt((Long) instance.longValue());
        });
        put(ValueType.INT, BigDecimal.class, (instance, valueType) -> {
            return new SpeedyInt((Long) instance.longValue());
        });
        put(ValueType.FLOAT, float.class, (instance, valueType) -> {
            return new SpeedyDouble(instance.doubleValue());
        });
        put(ValueType.FLOAT, Float.class, (instance, valueType) -> {
            return new SpeedyDouble(instance.doubleValue());
        });
        put(ValueType.FLOAT, double.class, (instance, valueType) -> {
            return new SpeedyDouble((Double) instance);
        });
        put(ValueType.FLOAT, Double.class, (instance, valueType) -> {
            return new SpeedyDouble((Double) instance);
        });
        put(ValueType.FLOAT, BigDecimal.class, (instance, valueType) -> {
            return new SpeedyDouble(instance.doubleValue());
        });
        put(ValueType.FLOAT, BigInteger.class, (instance, valueType) -> {
            return new SpeedyDouble(instance.doubleValue());
        });
        put(ValueType.BOOL, boolean.class, (instance, valueType) -> {
            return new SpeedyBoolean((Boolean) instance);
        });
        put(ValueType.BOOL, Boolean.class, (instance, valueType) -> {
            return new SpeedyBoolean((Boolean) instance);
        });
        put(ValueType.DATE, java.sql.Date.class, (instance, valueType) -> {
            java.sql.Date sqlDate = (java.sql.Date) instance;
            return new SpeedyDate(sqlDate.toLocalDate());
        });
        put(ValueType.DATE, Date.class, (instance, valueType) -> {
            Date kdate = (Date) instance;
            LocalDate localDate = kdate.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
            return new SpeedyDate(localDate);
        });
        put(ValueType.ZONED_DATE_TIME, OffsetDateTime.class, (instance, valueType) -> {
            OffsetDateTime offsetDateTime = instance;
            return new SpeedyZonedDateTime(offsetDateTime.toZonedDateTime());
        });
        put(ValueType.ZONED_DATE_TIME, ZonedDateTime.class, (instance, valueType) -> {
            ZonedDateTime zonedDateTime = instance;
            return new SpeedyZonedDateTime(zonedDateTime);
        });
        put(ValueType.ZONED_DATE_TIME, Instant.class, (instance, valueType) -> {
            Instant kdate = (Instant) instance;
            ZonedDateTime zonedDateTime = kdate.atZone(ZoneId.of("UTC"));
            return new SpeedyZonedDateTime(zonedDateTime);
        });
        put(ValueType.DATE_TIME, Instant.class, (instance, valueType) -> {
            Instant kdate = (Instant) instance;
            LocalDateTime localDateTime = kdate.atZone(ZoneId.of("UTC")).toLocalDateTime();
            return new SpeedyDateTime(localDateTime);
        });
//        put(ValueType.TIME, Instant.class, (instance, valueType) -> {
//            Instant kdate = (Instant) instance;
//            LocalDateTime localDateTime = kdate.atZone(ZoneId.systemDefault()).toLocalDateTime();
//            return new SpeedyDateTime(localDateTime);
//        });
        put(ValueType.DATE, LocalDate.class, (instance, valueType) -> {
            LocalDate kdate = (LocalDate) instance;
            return new SpeedyDate(kdate);
        });
        put(ValueType.DATE_TIME, LocalDateTime.class, (instance, valueType) -> {
            LocalDateTime kdate = (LocalDateTime) instance;
            return new SpeedyDateTime(kdate);
        });
        put(ValueType.DATE_TIME, Timestamp.class, (instance, valueType) -> {
            Timestamp kdate = (Timestamp) instance;
            LocalDateTime localDateTime = kdate.toLocalDateTime();
            return new SpeedyDateTime(localDateTime);
        });
        put(ValueType.TIME, LocalTime.class, (instance, valueType) -> {
            return new SpeedyTime(instance);
        });
        put(ValueType.TIME, Time.class, (instance, valueType) -> {
            return new SpeedyTime(instance.toLocalTime());
        });
        put(ValueType.ZONED_DATE_TIME, Timestamp.class, (instance, valueType) -> {
            Timestamp kdate = (Timestamp) instance;
            ZonedDateTime zonedDateTime = kdate.toLocalDateTime()
                    .atZone(ZoneId.systemDefault());
            return new SpeedyZonedDateTime(zonedDateTime);
        });
    }

    public static void convertAndSetField(SpeedyEntity entity, Field field, Object instance) throws SpeedyHttpException {
        Class<?> fieldType = field.getType();
        try {
            Object fieldValue = field.get(instance);
            EntityMetadata entityMetadata = entity.getMetadata();
            FieldMetadata fieldMetadata = entityMetadata.field(field.getName());
            ValueType valueType = fieldMetadata.getValueType();
            SpeedyValue speedyValue = convert(fieldType, valueType, fieldValue);
            entity.put(field.getName(), speedyValue);
        } catch (IllegalAccessException e) {
            throw new ConversionException("", e);
        }
    }

    /**
     * Converts a composite Java class to a SpeedyValue entity by mapping fields.
     * This is the opposite of the convertToCompositeClass method in SpeedyValue2JavaType.
     *
     * @param instance The Java object instance to convert
     * @param entity   The SpeedyEntity to update
     * @return The converted SpeedyEntity or null if conversion is not possible
     * @throws SpeedyHttpException If conversion fails
     */
    public static SpeedyEntity convertFromCompositeClass(Object instance, SpeedyEntity entity) throws SpeedyHttpException {
        if (instance == null) {
            return null;
        }

        try {
            EntityMetadata entityMetadata = entity.getMetadata();
            // Get all fields of the source class
            Class<?> clazz = instance.getClass();

            // Create a map of source fields for efficient lookup
            Map<String, Field> fieldMap = java.util.Arrays.stream(clazz.getDeclaredFields())
                    .collect(Collectors.toMap(Field::getName, Function.identity()));

            // Loop through only the fields known to Speedy (fields in entity metadata)
            for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
                String fieldName = fieldMetadata.getOutputPropertyName();

                // Check if the source class has this field
                if (!fieldMap.containsKey(fieldName)) {
                    continue;
                }

                // Check if the field is readable
                BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(instance);
                if (!wrapper.isReadableProperty(fieldName)) {
                    continue;
                }

                try {
                    // Get the field value from the source object
                    Object fieldValue = wrapper.getPropertyValue(fieldName);

                    // Get the field metadata
                    Class<?> fieldType = wrapper.getPropertyType(fieldName);
                    ValueType valueType = fieldMetadata.getValueType();

                    if (fieldValue == null) {
                        // If the field value is null, preserve the existing value in the entity
                        // Only set to SpeedyNull if the entity doesn't already have a value for this field
                        if (!entity.has(fieldMetadata)) {
                            entity.put(fieldMetadata, SpeedyNull.SPEEDY_NULL);
                        }
                        // If the entity already has a value, we preserve it by not setting anything
                    } else if (fieldMetadata.isAssociation()) {
                        // Handle associations/composite object fields
                        EntityMetadata assocMd = fieldMetadata.getAssociationMetadata();
                        SpeedyEntity child = entity.has(fieldMetadata) && entity.isObject() ?
                                entity.get(fieldMetadata).asObject() : new SpeedyEntity(assocMd);
                        // Recursively map the associated object
                        convertFromCompositeClass(fieldValue, child);
                        entity.put(fieldMetadata, child);
                    } else {
                        // Convert the field value to SpeedyValue
                        SpeedyValue speedyValue = convert(fieldType, valueType, fieldValue);
                        // Set the field value in the entity (avoid overwriting with SpeedyNull when already present)
                        if (!(speedyValue instanceof SpeedyNull) || !entity.has(fieldMetadata)) {
                            entity.put(fieldMetadata, speedyValue);
                        }
                    }
                } catch (Exception e) {
                    // Skip the field if conversion fails
                    throw new ConversionException(
                            String.format("Failed to convert field %s in class %s: %s",
                                    fieldName, clazz.getSimpleName(), e.getMessage()), e);
                }
            }

            return entity;
        } catch (Exception e) {
            throw new ConversionException(
                    String.format("Cannot convert %s to SpeedyEntity", instance), e);
        }
    }
}
