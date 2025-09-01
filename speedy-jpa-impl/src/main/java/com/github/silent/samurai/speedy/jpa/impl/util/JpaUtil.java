package com.github.silent.samurai.speedy.jpa.impl.util;

import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class JpaUtil {

    static Class<?> getIdClassType(EntityType<?> entityType) {
        Class<?> entityClassType = entityType.getBindableJavaType();
        IdClass idClass = AnnotationUtils.getAnnotation(entityClassType, IdClass.class);
        if (idClass == null) {
            return entityType.getIdType().getJavaType();
        }
        return idClass.value();
    }

    public static String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Entity.class)) {
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            if (tableAnnotation != null) {
                return tableAnnotation.name();
            }
        }
        return null;
    }

    public static Field getField(Class<?> clazz, String fieldName) throws IllegalStateException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException var3) {
            if (clazz.getSuperclass() != null) {
                return getField(clazz.getSuperclass(), fieldName);
            } else {
                throw new IllegalStateException("Could not locate field '" + fieldName + "' on class " + clazz);
            }
        }
    }

    public static Class<?> resolveGenericFieldType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                return (Class<?>) actualTypeArguments[0];
            }
        }
        throw new RuntimeException("Field is not Generic");
    }
}
