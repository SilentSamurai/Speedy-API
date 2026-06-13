package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/// Converts between {@link SpeedyValue} representations and plain Java objects.
/// Primitive values (text, number, boolean, enum, date, etc.) are handled via
/// {@link #asJavaObject(SpeedyValue)} using a {@link JavaTypeRegistry}.
/// Entity values are mapped to composite Java classes through
/// {@link #toJavaEntity(SpeedyEntity, Class)}, which uses Spring's {@code BeanWrapper}
/// to populate fields by name, including nested association traversal.
public class SpeedySerializer {

    /// The Java-type registry used for all {@code SpeedyValue <-> Java} conversions.
    /// Set at construction time and never changed.
    private final JavaTypeRegistry javaTypeRegistry;

    /// Creates a serializer that delegates all conversions to the supplied registry.
    ///
    /// @param javaTypeRegistry the registry to use for every conversion
    public SpeedySerializer(JavaTypeRegistry javaTypeRegistry) {
        this.javaTypeRegistry = javaTypeRegistry;
    }

    private Object toJavaField(SpeedyValue value, FieldMetadata fieldMetadata, Class<?> clazz) throws SpeedyHttpException {
        ValueType valueType = value.getValueType();
        if (value instanceof SpeedyNull) {
            return null;
        }

        if (javaTypeRegistry.canToJava(valueType, clazz)) {
            return javaTypeRegistry.toJava(value, clazz);
        } else if (clazz.isEnum()) {
            return CommonUtil.convertToEnum((Class<? extends Enum>) clazz, value);
        }

        return null;
    }

    /// Converts a SpeedyValue to its natural Java type (Boolean, String, Long, etc.).
    /// The caller must cast the result to the expected java type.
    public Object asJavaObject(SpeedyValue value) {
        if (value == null || value instanceof SpeedyNull) {
            return null;
        }

        return switch (value.getValueType()) {
            case NULL -> null;
            case BOOL -> value.asBoolean();
            case TEXT -> value.asText();
            case ENUM -> value.asEnum();
            case ENUM_ORD -> value.asEnumOrd();
            case INT -> value.asInt();
            case FLOAT -> value.asDouble();
            case DATE -> value.asDate();
            case TIME -> value.asTime();
            case DATE_TIME -> value.asDateTime();
            case ZONED_DATE_TIME -> value.asZonedDateTime();
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
    public <T> T toJavaEntity(SpeedyEntity value, Class<T> clazz) throws SpeedyHttpException {
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
