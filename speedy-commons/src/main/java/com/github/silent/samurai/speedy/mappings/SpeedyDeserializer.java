package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class SpeedyDeserializer {
    // All conversion logic is delegated to TypeConverterRegistry; legacy converters removed.
    // Obsolete façade methods (get/has/put) removed – callers should use TypeConverterRegistry directly.

    public static SpeedyValue fromJavaObject(FieldMetadata fieldMetadata, Object instance) throws SpeedyHttpException {
        ValueType valueType = fieldMetadata.getValueType();
        if (instance == null) {
            return SpeedyNull.SPEEDY_NULL;
        }

        Class<?> clazz = instance.getClass();

        if (clazz.isEnum() && instance instanceof Enum<?> enumInstance) {
            switch (valueType) {
                case TEXT, ENUM -> {
                    return new SpeedyText(enumInstance.name());
                }
                case INT, ENUM_ORD -> {
                    return new SpeedyInt((long) enumInstance.ordinal());
                }
                default -> {
                    throw new ConversionException(
                            String.format("Cannot convert enum %s to %s",
                                    enumInstance.getClass().getSimpleName(), valueType));
                }
            }
        }
        if (!TypeConverterRegistry.canToSpeedy(valueType, clazz)) {
            return SpeedyNull.SPEEDY_NULL;
        }

        return TypeConverterRegistry.toSpeedy(instance, valueType);
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
    public static SpeedyEntity updateEntity(Object instance, SpeedyEntity entity) throws SpeedyHttpException {
        if (instance == null) {
            return null;
        }

        try {
            EntityMetadata entityMetadata = entity.getMetadata();
            Class<?> clazz = instance.getClass();
            BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(instance);
            Map<String, Field> fieldMap = Arrays.stream(clazz.getDeclaredFields())
                    .collect(Collectors.toMap(Field::getName, f -> f));

            for (FieldMetadata fm : entityMetadata.getAllFields()) {
                String name = fm.getOutputPropertyName();
                if (!fieldMap.containsKey(name) || !wrapper.isReadableProperty(name)) continue;

                try {
                    Object srcVal = wrapper.getPropertyValue(name);

                    if (srcVal == null) {
                        if (!entity.has(fm)) entity.put(fm, SpeedyNull.SPEEDY_NULL);
                        continue;
                    }

                    if (fm.isAssociation()) {
                        EntityMetadata assocMd = fm.getAssociationMetadata();
                        SpeedyEntity child = entity.has(fm) && entity.isObject() ?
                                entity.get(fm).asObject() : new SpeedyEntity(assocMd);
                        updateEntity(srcVal, child);
                        entity.put(fm, child);
                    } else {
                        SpeedyValue sv = fromJavaObject(fm, srcVal);
                        if (!(sv instanceof SpeedyNull) || !entity.has(fm)) entity.put(fm, sv);
                    }
                } catch (Exception e) {
                    throw new ConversionException(
                            String.format("Failed to convert field %s in class %s: %s",
                                    name, clazz.getSimpleName(), e.getMessage()), e);
                }
            }

            return entity;
        } catch (Exception e) {
            throw new ConversionException(
                    String.format("Cannot convert %s to SpeedyEntity", instance), e);
        }
    }
}
