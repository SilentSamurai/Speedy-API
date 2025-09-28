package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.Arrays;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Collectors;

public class SpeedySerializer {

    // All converter logic is delegated to TypeConverterRegistry; get/has removed.

    private static Object toJavaField(SpeedyValue value, FieldMetadata fieldMetadata, Class<?> clazz) throws SpeedyHttpException {
        ValueType valueType = value.getValueType();
        // handle null
        if (value instanceof SpeedyNull) {
            return null;
        }

        // First, try the registry – that covers primitives, wrappers, temporal types, etc.
        if (TypeConverterRegistry.canToJava(valueType, clazz)) {
            // TypeConverterRegistry already wraps primitives to their corresponding wrapper types,
            // and BeanWrapper will handle boxing/unboxing on assignment later.
            return TypeConverterRegistry.toJava(value, clazz);
        } else if (clazz.isEnum()) {
            // Safe because convertToEnum returns the right enum subtype
            return CommonUtil.convertToEnum((Class<? extends Enum>) clazz, value);
        }

        // No suitable converter found
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T asJavaObject(SpeedyValue value) {
        if (value == null || value instanceof SpeedyNull) {
            return null;
        }

        return switch (value.getValueType()) {
            case NULL -> null;
            case BOOL -> (T) value.asBoolean();
            case TEXT -> (T) value.asText();
            case ENUM -> (T) value.asEnum();
            case ENUM_ORD -> (T) value.asEnumOrd();
            case INT -> (T) value.asInt();
            case FLOAT -> (T) value.asDouble();
            case DATE -> (T) value.asDate();
            case TIME -> (T) value.asTime();
            case DATE_TIME -> (T) value.asDateTime();
            case ZONED_DATE_TIME -> (T) value.asZonedDateTime();
            case OBJECT, COLLECTION -> throw new ConversionException(
                    String.format("Cannot convert %s to %s", value, value.getValueType().name())
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
    public static <T> T toJavaEntity(SpeedyEntity value, Class<T> clazz) throws SpeedyHttpException {
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
            // Create instance of target class
            T instance = clazz.getDeclaredConstructor().newInstance();
            BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(instance);
            Map<String, Field> fieldMap = Arrays.stream(clazz.getDeclaredFields())
                    .collect(Collectors.toMap(Field::getName, f -> f));

            for (FieldMetadata fm : entity.getMetadata().getAllFields()) {
                String name = fm.getOutputPropertyName();
                if (!entity.has(fm) || !fieldMap.containsKey(name)) continue;

                try {
                    SpeedyValue fv = entity.get(fm);
                    Object val;
                    if (fv instanceof SpeedyNull) {
                        val = null;
                    } else if (fm.isAssociation() && fv.isObject()) {
                        val = toJavaEntity(fv.asObject(), fieldMap.get(name).getType());
                    } else {
                        val = toJavaField(fv, fm, fieldMap.get(name).getType());
                    }

                    if (wrapper.isWritableProperty(name)) wrapper.setPropertyValue(name, val);
                } catch (Exception e) {
                    throw new ConversionException(
                            String.format("Failed to convert field %s in class %s: %s",
                                    name, clazz.getSimpleName(), e.getMessage()), e);
                }
            }

            return instance;
        } catch (Exception e) {
            throw new ConversionException(
                    String.format("Cannot convert %s to composite class %s", value, clazz.getSimpleName()), e);
        }
    }
}
