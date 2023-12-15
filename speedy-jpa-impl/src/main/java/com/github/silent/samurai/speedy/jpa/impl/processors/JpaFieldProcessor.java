package com.github.silent.samurai.speedy.jpa.impl.processors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.silent.samurai.speedy.annotations.SpeedyIgnore;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.jpa.impl.metamodel.JpaEntityMetadata;
import com.github.silent.samurai.speedy.jpa.impl.metamodel.JpaFieldMetadata;
import com.github.silent.samurai.speedy.jpa.impl.metamodel.JpaKeyFieldMetadata;
import com.github.silent.samurai.speedy.utils.ValueTypeUtil;
import org.hibernate.annotations.Formula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.JoinColumn;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

public class JpaFieldProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaFieldProcessor.class);

    public static Map<String, Method> findGetterSetter(Class<?> clazz, String fieldName) {
        Map<String, Method> getterSetter = new HashMap<>();
        for (Method method : clazz.getMethods()) {
            if (method.getName().toLowerCase().endsWith(fieldName.toLowerCase())) {
                try {
                    if ((method.getName().startsWith("get")) && (method.getName().length() == (fieldName.length() + 3))) {
                        getterSetter.put("GET", method);
                    }
                    if ((method.getName().startsWith("set")) && (method.getName().length() == (fieldName.length() + 3))) {
                        getterSetter.put("SET", method);
                    }
                } catch (IllegalStateException e) {
                    LOGGER.error("Could not determine method: {} ", fieldName, e);
                }
            }
        }
        return getterSetter;
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

    static Class<?> resolveGenericFieldType(Field field) {
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

    static void processAssociations(JpaFieldMetadata fieldMetadata, Map<Class<?>, JpaEntityMetadata> typeMap) {
        if (fieldMetadata.isAssociation()) {
            Class<?> fieldType = fieldMetadata.getFieldType();
            if (fieldMetadata.isCollection()) {
                fieldType = resolveGenericFieldType(fieldMetadata.getField());
            }
            if (typeMap.containsKey(fieldType)) {
                fieldMetadata.setAssociationMetadata(typeMap.get(fieldType));
            } else {
                throw new RuntimeException("Entity not found " + fieldType);
            }
        }
    }

    public static JpaFieldMetadata processField(Attribute<?, ?> attribute,
                                                Class<?> entityClass,
                                                JpaEntityMetadata entityMetadata) {
        Member member = attribute.getJavaMember();
        JpaFieldMetadata fieldMetadata;

        if (attribute instanceof SingularAttribute && ((SingularAttribute<?, ?>) attribute).isId()) {
            JpaKeyFieldMetadata jpaKeyFieldMetadata = new JpaKeyFieldMetadata();
            jpaKeyFieldMetadata.setId(true);
            if (entityMetadata.hasCompositeKey()) {
                Class<?> idClass = entityMetadata.getKeyClass();
                Field idClassField = getField(idClass, member.getName());
                jpaKeyFieldMetadata.setIdClassField(idClassField);
                Map<String, Method> getterSetter = findGetterSetter(idClass, idClassField.getName());
                jpaKeyFieldMetadata.setIdClassGetter(getterSetter.get("GET"));
                jpaKeyFieldMetadata.setIdClassSetter(getterSetter.get("SET"));
            } else {
                Field idClassField = getField(entityClass, member.getName());
                jpaKeyFieldMetadata.setIdClassField(idClassField);
                Map<String, Method> getterSetter = findGetterSetter(entityClass, idClassField.getName());
                jpaKeyFieldMetadata.setIdClassGetter(getterSetter.get("GET"));
                jpaKeyFieldMetadata.setIdClassSetter(getterSetter.get("SET"));
            }
            fieldMetadata = jpaKeyFieldMetadata;
        } else {
            fieldMetadata = new JpaFieldMetadata();
        }
        fieldMetadata.setEntityMetadata(entityMetadata);
        fieldMetadata.setJpaAttribute(attribute);
        fieldMetadata.setClassFieldName(member.getName());
        fieldMetadata.setFieldType(attribute.getJavaType());
        if (attribute.getJavaMember() instanceof Field) {
            fieldMetadata.setField((Field) attribute.getJavaMember());
        } else {
            fieldMetadata.setField(getField(entityClass, member.getName()));
        }

        fieldMetadata.setInsertable(true);
        fieldMetadata.setUnique(false);
        fieldMetadata.setUpdatable(true);
        fieldMetadata.setNullable(false);
        fieldMetadata.setRequired(false);
        fieldMetadata.setSerializable(true);
        fieldMetadata.setDeserializable(true);

        Column columnAnnotation = AnnotationUtils.getAnnotation(fieldMetadata.getField(), Column.class);
        if (columnAnnotation != null) {
            fieldMetadata.setDbColumnName(columnAnnotation.name());
            fieldMetadata.setInsertable(columnAnnotation.insertable());
            fieldMetadata.setUnique(columnAnnotation.unique());
            fieldMetadata.setUpdatable(columnAnnotation.updatable());
            fieldMetadata.setNullable(columnAnnotation.nullable());

        }

        JoinColumn joinColumnAnnotation = AnnotationUtils.getAnnotation(fieldMetadata.getField(), JoinColumn.class);
        if (joinColumnAnnotation != null) {
            fieldMetadata.setDbColumnName(joinColumnAnnotation.name());
            fieldMetadata.setInsertable(joinColumnAnnotation.insertable());
            fieldMetadata.setUnique(joinColumnAnnotation.unique());
            fieldMetadata.setUpdatable(joinColumnAnnotation.updatable());
            fieldMetadata.setNullable(joinColumnAnnotation.nullable());
        }

        GeneratedValue generatedValueAnnotation = AnnotationUtils.getAnnotation(fieldMetadata.getField(), GeneratedValue.class);
        if (generatedValueAnnotation != null) {
            fieldMetadata.setInsertable(false);
            fieldMetadata.setUpdatable(false);
            fieldMetadata.setNullable(false);
            fieldMetadata.setDeserializable(false);
        }

        Formula formulaAnnotation = AnnotationUtils.getAnnotation(fieldMetadata.getField(), Formula.class);
        if (formulaAnnotation != null) {
            fieldMetadata.setInsertable(false);
            fieldMetadata.setUpdatable(false);
            fieldMetadata.setNullable(false);
            fieldMetadata.setNullable(false);
            fieldMetadata.setDeserializable(false);
        }

        JsonProperty propertyAnnotation = AnnotationUtils.getAnnotation(fieldMetadata.getField(), JsonProperty.class);
        fieldMetadata.setOutputPropertyName(fieldMetadata.getClassFieldName());
        if (propertyAnnotation != null) {
            fieldMetadata.setOutputPropertyName(propertyAnnotation.value());
        }

        JsonIgnore jsonIgnore = AnnotationUtils.getAnnotation(fieldMetadata.getField(), JsonIgnore.class);
        if (jsonIgnore != null) {
            fieldMetadata.setSerializable(false);
            fieldMetadata.setDeserializable(false);
        }

        SpeedyIgnore speedyIgnore = AnnotationUtils.getAnnotation(fieldMetadata.getField(), SpeedyIgnore.class);
        if (speedyIgnore != null) {
            switch (speedyIgnore.value()) {
                case READ:
                    fieldMetadata.setSerializable(false);
                    break;
                case WRITE:
                    fieldMetadata.setDeserializable(false);
                    break;
                case ALL:
                    fieldMetadata.setSerializable(false);
                    fieldMetadata.setDeserializable(false);
                    break;
            }
            fieldMetadata.setIgnoreProperty(speedyIgnore.value());
        }

        if (!fieldMetadata.isNullable() && fieldMetadata.isDeserializable()) {
            fieldMetadata.setRequired(true);
        }

        // MZ: Find the correct method
        Map<String, Method> getterSetter = findGetterSetter(entityClass, member.getName());
        fieldMetadata.setGetter(getterSetter.get("GET"));
        fieldMetadata.setSetter(getterSetter.get("SET"));
        fieldMetadata.setValueType(ValueTypeUtil.fromClass(fieldMetadata.getFieldType()));
        return fieldMetadata;
    }
}
