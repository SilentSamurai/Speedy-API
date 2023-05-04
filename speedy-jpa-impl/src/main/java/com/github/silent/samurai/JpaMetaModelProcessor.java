package com.github.silent.samurai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.silent.samurai.annotations.SpeedyIgnore;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.metamodel.JpaEntityMetadata;
import com.github.silent.samurai.metamodel.JpaFieldMetadata;
import com.github.silent.samurai.metamodel.JpaKeyFieldMetadata;
import org.hibernate.annotations.Formula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import javax.persistence.*;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class JpaMetaModelProcessor implements MetaModelProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaMetaModelProcessor.class);

    private final Map<String, JpaEntityMetadata> entityMap = new HashMap<>();
    private final Map<Class<?>, JpaEntityMetadata> typeMap = new HashMap<>();

    public JpaMetaModelProcessor(EntityManagerFactory entityManagerFactory) {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();
        for (EntityType<?> entityType : entities) {
            this.addEntity(entityType);
            LOGGER.info("registering resources {}", entityType.getName());
        }
        processAssociations();
    }

    public static JpaFieldMetadata findFieldMetadata(Attribute<?, ?> attribute, Class<?> entityClass) {
        Member member = attribute.getJavaMember();
        JpaFieldMetadata fieldMetadata;

        if (attribute instanceof SingularAttribute && ((SingularAttribute<?, ?>) attribute).isId()) {
            JpaKeyFieldMetadata jpaKeyFieldMetadata = new JpaKeyFieldMetadata();
            jpaKeyFieldMetadata.setId(true);
            fieldMetadata = jpaKeyFieldMetadata;
        } else {
            fieldMetadata = new JpaFieldMetadata();
        }

        fieldMetadata.setJpaAttribute(attribute);
        fieldMetadata.setClassFieldName(member.getName());
        fieldMetadata.setFieldType(attribute.getJavaType());
        if (attribute.getJavaMember() instanceof Field) {
            fieldMetadata.setField((Field) attribute.getJavaMember());
        } else {
            fieldMetadata.setField(JpaMetaModelProcessor.getField(entityClass, member.getName()));
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
            fieldMetadata.setIgnoreType(speedyIgnore.value());
        }

        if (!fieldMetadata.isNullable() && fieldMetadata.isDeserializable()) {
            fieldMetadata.setRequired(true);
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
                } catch (IllegalStateException e) {
                    LOGGER.error("Could not determine method: {} ", member, e);
                }
            }
        }
        return fieldMetadata;
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

    private void processAssociations() {
        for (JpaEntityMetadata entityMetadata : entityMap.values()) {
            for (JpaFieldMetadata fieldMetadata : entityMetadata.getFieldMap().values()) {
                if (fieldMetadata.isAssociation()) {
                    Class<?> fieldType = fieldMetadata.getFieldType();
                    if (fieldMetadata.isCollection()) {
                        Type genericType = fieldMetadata.getField().getGenericType();
                        if (genericType instanceof ParameterizedType) {
                            ParameterizedType parameterizedType = (ParameterizedType) genericType;
                            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                            if (actualTypeArguments.length > 0) {
                                fieldType = (Class<?>) actualTypeArguments[0];
                            }
                        }
                    }
                    if (typeMap.containsKey(fieldType)) {
                        fieldMetadata.setAssociationMetadata(typeMap.get(fieldType));
                    } else {
                        throw new RuntimeException("Entity not found " + fieldType);
                    }
                }
            }
        }
    }

    private Class<?> getIdClassType(EntityType<?> entityType) {
        Class<?> entityClassType = entityType.getBindableJavaType();
        IdClass idClass = AnnotationUtils.getAnnotation(entityClassType, IdClass.class);
        if (idClass == null) {
            return entityType.getIdType().getJavaType();
        }
        return idClass.value();
    }

    public void addEntity(EntityType<?> entityType) {
        JpaEntityMetadata entityMetadata = new JpaEntityMetadata();
        entityMetadata.setName(entityType.getName());
        entityMetadata.setJpaEntityType(entityType);
        entityMetadata.setEntityClass(entityType.getBindableJavaType());
        entityMetadata.setKeyClass(getIdClassType(entityType));
        entityMetadata.setHasCompositeKey(!entityType.hasSingleIdAttribute());

        for (Attribute<?, ?> attribute : entityType.getAttributes()) {
            JpaFieldMetadata memberMetadata = findFieldMetadata(attribute, entityType.getJavaType());
            entityMetadata.addFieldMetadata(memberMetadata);
        }
        entityMap.put(entityType.getName(), entityMetadata);
        typeMap.put(entityMetadata.getEntityClass(), entityMetadata);
    }

    @Override
    public Collection<EntityMetadata> getAllEntityMetadata() {
        return entityMap.values().stream().map(em -> (EntityMetadata) em).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean hasEntityMetadata(Class<?> entityType) {
        return typeMap.containsKey(entityType);
    }

    @Override
    public EntityMetadata findEntityMetadata(Class<?> entityType) throws NotFoundException {
        return typeMap.get(entityType);
    }

    @Override
    public boolean hasEntityMetadata(String entityName) {
        return entityMap.containsKey(entityName);
    }

    @Override
    public EntityMetadata findEntityMetadata(String entityName) throws NotFoundException {
        if (entityMap.containsKey(entityName)) {
            return entityMap.get(entityName);
        }
        throw new NotFoundException(entityName);
    }

    @Override
    public FieldMetadata findFieldMetadata(String entityName, String fieldName) throws NotFoundException {
        EntityMetadata entityMetadata = findEntityMetadata(entityName);
        return entityMetadata.field(entityName);
    }
}
