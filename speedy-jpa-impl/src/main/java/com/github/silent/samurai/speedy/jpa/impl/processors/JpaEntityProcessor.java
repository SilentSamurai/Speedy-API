package com.github.silent.samurai.speedy.jpa.impl.processors;

import com.github.silent.samurai.speedy.annotations.SpeedyAction;
import com.github.silent.samurai.speedy.jpa.impl.metamodel.JpaEntityMetadata;
import com.github.silent.samurai.speedy.jpa.impl.metamodel.JpaFieldMetadata;
import org.springframework.core.annotation.AnnotationUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import java.util.Map;

public class JpaEntityProcessor {


    static void processAssociations(JpaEntityMetadata entityMetadata, Map<Class<?>, JpaEntityMetadata> typeMap) {
        for (JpaFieldMetadata fieldMetadata : entityMetadata.getFieldMap().values()) {
            if (fieldMetadata.isAssociation()) {
                JpaFieldProcessor.processAssociations(fieldMetadata, typeMap);
            }
        }
    }

    public static JpaEntityMetadata processEntity(EntityType<?> entityType) {
        JpaEntityMetadata entityMetadata = new JpaEntityMetadata();
        entityMetadata.setName(entityType.getName());
        entityMetadata.setJpaEntityType(entityType);
        entityMetadata.setEntityClass(entityType.getBindableJavaType());
        entityMetadata.setKeyClass(getIdClassType(entityType));
        entityMetadata.setHasCompositeKey(!entityType.hasSingleIdAttribute());
        entityMetadata.setTableName(getTableName(entityType.getJavaType()));

        SpeedyAction annotation = entityType.getBindableJavaType().getAnnotation(SpeedyAction.class);
        if (annotation != null) {
            entityMetadata.setActionType(annotation.value());
        }

        for (Attribute<?, ?> attribute : entityType.getAttributes()) {
            JpaFieldMetadata memberMetadata = JpaFieldProcessor.processField(attribute,
                    entityType.getJavaType(), entityMetadata);

            if (memberMetadata.getDbColumnName() == null) {
                continue;
            }

            entityMetadata.addFieldMetadata(memberMetadata);
        }
        return entityMetadata;
    }

    private static Class<?> getIdClassType(EntityType<?> entityType) {
        Class<?> entityClassType = entityType.getBindableJavaType();
        IdClass idClass = AnnotationUtils.getAnnotation(entityClassType, IdClass.class);
        if (idClass == null) {
            return entityType.getIdType().getJavaType();
        }
        return idClass.value();
    }

    private static String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Entity.class)) {
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            if (tableAnnotation != null) {
                return tableAnnotation.name();
            }
        }
        return null;
    }

}
