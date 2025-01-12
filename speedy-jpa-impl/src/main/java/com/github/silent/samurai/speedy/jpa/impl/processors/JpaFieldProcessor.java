package com.github.silent.samurai.speedy.jpa.impl.processors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.silent.samurai.speedy.annotations.SpeedyAction;
import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaFieldMetadata;
import com.github.silent.samurai.speedy.jpa.impl.metamodel.JpaEntityMetadata;
import com.github.silent.samurai.speedy.jpa.impl.metamodel.JpaFieldMetadata;
import com.github.silent.samurai.speedy.jpa.impl.metamodel.JpaKeyFieldMetadata;
import com.github.silent.samurai.speedy.mappings.JavaType2ValueType;
import com.github.silent.samurai.speedy.utils.ValueTypeUtil;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.SingularAttribute;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.silent.samurai.speedy.enums.ActionType.*;

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
                JpaEntityMetadata jpaEntityMetadata = typeMap.get(fieldType);
                fieldMetadata.setAssociationMetadata(jpaEntityMetadata);

                IJpaFieldMetadata keyFieldMetadata = (IJpaFieldMetadata) jpaEntityMetadata
                        .getKeyFields()
                        .stream()
                        .findAny()
                        .orElse(null);
                fieldMetadata.setAssociatedFieldMetadata(keyFieldMetadata);
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
            boolean isUuidGenerationRequired =
                    generatedValueAnnotation.generator().toUpperCase().contains("UUID") ||
                            generatedValueAnnotation.strategy() == GenerationType.UUID;
            if (fieldMetadata instanceof KeyFieldMetadata && isUuidGenerationRequired) {
                JpaKeyFieldMetadata keyFieldMetadata = (JpaKeyFieldMetadata) fieldMetadata;
                keyFieldMetadata.setGenerateIdKeys(true);
                // uuid is not required to be present inside the speedy framework, string is enough.
                fieldMetadata.setFieldType(String.class);
            }
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

        SpeedyAction speedyAction = AnnotationUtils.getAnnotation(fieldMetadata.getField(), SpeedyAction.class);
        if (speedyAction != null) {
            Set<ActionType> actionTypesSet = Arrays.stream(speedyAction.value()).collect(Collectors.toSet());

            // give as less permission as you can
            fieldMetadata.setSerializable(false);
            fieldMetadata.setInsertable(false);
            fieldMetadata.setUpdatable(false);
            fieldMetadata.setDeserializable(false);

            if (actionTypesSet.contains(CREATE)) {
                fieldMetadata.setInsertable(true);
                fieldMetadata.setSerializable(true);
            }
            if (actionTypesSet.contains(UPDATE)) {
                fieldMetadata.setUpdatable(true);
                fieldMetadata.setSerializable(true);
            }
            if (actionTypesSet.contains(DELETE)) {
                // todo: figure out what logic can be done here
                fieldMetadata.setSerializable(true);
            }
            if (actionTypesSet.contains(READ)) {
                fieldMetadata.setDeserializable(true);
            }
            if (actionTypesSet.contains(ALL)) {
                fieldMetadata.setInsertable(true);
                fieldMetadata.setUpdatable(true);
                fieldMetadata.setSerializable(true);
                fieldMetadata.setDeserializable(true);
            }
        }

        Enumerated enumerated = AnnotationUtils.getAnnotation(fieldMetadata.getField(), Enumerated.class);
        if (enumerated != null) {
            EnumType value = enumerated.value();
            switch (value) {
                case STRING -> fieldMetadata.setValueType(ValueType.TEXT);
                case ORDINAL -> fieldMetadata.setValueType(ValueType.INT);
            }
        } else {
            fieldMetadata.setValueType(JavaType2ValueType.fromClass(fieldMetadata.getFieldType()));
        }

        if (!fieldMetadata.isNullable() && fieldMetadata.isDeserializable()) {
            fieldMetadata.setRequired(true);
        }

        // MZ: Find the correct method
        Map<String, Method> getterSetter = findGetterSetter(entityClass, member.getName());
        fieldMetadata.setGetter(getterSetter.get("GET"));
        fieldMetadata.setSetter(getterSetter.get("SET"));


        return fieldMetadata;
    }
}
