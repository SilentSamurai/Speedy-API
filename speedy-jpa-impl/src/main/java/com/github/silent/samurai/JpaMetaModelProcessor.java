package com.github.silent.samurai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.silent.samurai.annotations.SpeedyCustomValidation;
import com.github.silent.samurai.annotations.SpeedyIgnore;
import com.github.silent.samurai.enums.IgnoreType;
import com.github.silent.samurai.exceptions.ResourceNotFoundException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.metamodel.JpaEntityMetadata;
import com.github.silent.samurai.metamodel.JpaFieldMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;

import javax.persistence.Column;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class JpaMetaModelProcessor implements MetaModelProcessor {

    public static Logger logger = LogManager.getLogger(JpaMetaModelProcessor.class);
    private final Map<String, EntityMetadata> entityMap = new HashMap<>();

    public JpaMetaModelProcessor(EntityManagerFactory entityManagerFactory) {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();
        for (EntityType<?> entityType : entities) {
            this.addEntity(entityType);
            logger.info("registering resources {}", entityType.getName());
        }
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

    public static JpaFieldMetadata findFieldMetadata(Attribute<?, ?> attribute, Class<?> entityClass) {
        Member member = attribute.getJavaMember();
        JpaFieldMetadata fieldMetadata = new JpaFieldMetadata();
        fieldMetadata.setJpaAttribute(attribute);
        fieldMetadata.setClassFieldName(member.getName());
        fieldMetadata.setFieldType(attribute.getJavaType());
        if (attribute instanceof SingularAttribute) {
            fieldMetadata.setId(((SingularAttribute<?, ?>) attribute).isId());
        } else {
            fieldMetadata.setId(false);
        }

        // MZ: Find the correct method
        for (Method method : entityClass.getMethods()) {
            if (method.getName().toLowerCase().endsWith(member.getName().toLowerCase())) {
                try {
                    if ((method.getName().startsWith("get")) && (method.getName().length() == (member.getName().length() + 3))) {
                        fieldMetadata.setGetter(method);
                    }
                    if ((method.getName().startsWith("set")) && (method.getName().length() == (member.getName().length() + 3))) {
                        fieldMetadata.setSetter(method);
                    }

                    fieldMetadata.setField(JpaMetaModelProcessor.getField(entityClass, member.getName()));
                    SpeedyCustomValidation annotation = AnnotationUtils.getAnnotation(fieldMetadata.getField(), SpeedyCustomValidation.class);
                    if (annotation != null) {
                        fieldMetadata.setCustomValidation(annotation.value());
                    }

                    JsonProperty propertyAnnotation = AnnotationUtils.getAnnotation(fieldMetadata.getField(), JsonProperty.class);
                    fieldMetadata.setOutputPropertyName(fieldMetadata.getClassFieldName());
                    if (propertyAnnotation != null) {
                        fieldMetadata.setOutputPropertyName(propertyAnnotation.value());
                    }

                    Column columnAnnotation = AnnotationUtils.getAnnotation(fieldMetadata.getField(), Column.class);
                    if (columnAnnotation != null) {
                        fieldMetadata.setDbColumnName(columnAnnotation.name());
                    }

                } catch (IllegalStateException e) {
                    logger.fatal("Could not determine method: {} ", member, e);
                }
            }
        }
        return fieldMetadata;
    }

    public void addEntity(EntityType<?> entityType) {
        JpaEntityMetadata entityMetadata = new JpaEntityMetadata();
        entityMetadata.setName(entityType.getName());
        entityMetadata.setJpaEntityType(entityType);
        entityMetadata.setEntityClass(entityType.getBindableJavaType());
        entityMetadata.setKeyClass(entityType.getIdType().getJavaType());
        for (Attribute<?, ?> attribute : entityType.getAttributes()) {
            JpaFieldMetadata memberMetadata = findFieldMetadata(attribute, entityType.getJavaType());
            SpeedyIgnore annotation = AnnotationUtils.getAnnotation(memberMetadata.getField(), SpeedyIgnore.class);
            if (annotation != null) {
                if (annotation.value() == IgnoreType.ALL) {
                    continue;
                }
                memberMetadata.setIgnoreType(annotation.value());
            }
            entityMetadata.getAllFields().add(memberMetadata);
            entityMetadata.getFieldMap().put(attribute.getName(), memberMetadata);
            if (memberMetadata.isId()) {
                entityMetadata.getKeyFields().add(attribute.getName());
            }

        }
        entityMap.put(entityType.getName(), entityMetadata);
    }

    @Override
    public EntityMetadata findEntityMetadata(String entityName) throws ResourceNotFoundException {
        if (entityMap.containsKey(entityName)) {
            return entityMap.get(entityName);
        }
        throw new ResourceNotFoundException(entityName);
    }

    @Override
    public FieldMetadata findFieldMetadata(String entityName, String fieldName) throws ResourceNotFoundException {
        EntityMetadata entityMetadata = findEntityMetadata(entityName);
        return entityMetadata.field(entityName);
    }
}
