package com.github.silent.samurai.speedy.jpa.impl.util;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaEntityMetadata;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyValueFactory;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommonUtil {


    public static SpeedyEntity fromJpaEntity(Object entity, EntityMetadata entityMetadata) throws Exception {
        return fromJpaEntityInner(entity, entityMetadata, true);
    }

    private static SpeedyEntity fromJpaEntityInner(Object entity, EntityMetadata entityMetadata, boolean goDeep) throws Exception {
        SpeedyEntity speedyEntity = new SpeedyEntity(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            Object fieldValue = fieldMetadata.getEntityFieldValue(entity);
            if (fieldValue == null) {
                speedyEntity.put(fieldMetadata, SpeedyValueFactory.fromNull());
                continue;
            }

            if (fieldMetadata.isAssociation()) {
                if (goDeep) {
                    if (fieldMetadata.isCollection()) {
                        Collection<?> collection = (Collection<?>) fieldValue;
                        Collection<SpeedyValue> collect = collection.stream().map(item -> {
                            EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                            try {
                                return fromJpaEntityInner(fieldValue, associationMetadata, false);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }).collect(Collectors.toList());
                        SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(collect);
                        speedyEntity.put(fieldMetadata, speedyCollection);
                    } else {
                        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                        SpeedyEntity ae = fromJpaEntityInner(fieldValue, associationMetadata, false);
                        speedyEntity.put(fieldMetadata, ae);
                    }
                }
            } else {
                if (fieldMetadata.isCollection()) {
                    Collection<?> collection = (Collection<?>) fieldValue;
                    List<SpeedyValue> collect = collection.stream().map(item -> {
                                try {
                                    return SpeedyValueFactory.basicToSpeedyValue(
                                            fieldMetadata.getFieldType(), fieldMetadata.getValueType(), item);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(Collectors.toList());
                    SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(collect);
                    speedyEntity.put(fieldMetadata, speedyCollection);
                } else {
                    SpeedyValue speedyValue = SpeedyValueFactory.basicToSpeedyValue(
                            fieldMetadata.getFieldType(), fieldMetadata.getValueType(), fieldValue);
                    speedyEntity.put(fieldMetadata, speedyValue);
                }
            }
        }
        return speedyEntity;
    }


    public static Object fromSpeedyEntity(SpeedyEntity speedyEntity, IJpaEntityMetadata entityMetadata, EntityManager entityManager) throws Exception {
        Object newEntityInstance = entityMetadata.createNewEntityInstance();
        updateFromSpeedyEntity(speedyEntity, newEntityInstance, entityMetadata, entityManager);
        return newEntityInstance;
    }

    public static void updateFromSpeedyEntity(SpeedyEntity speedyEntity,
                                              Object jpaEntityInstance,
                                              EntityMetadata entityMetadata,
                                              EntityManager entityManager) throws Exception {
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!speedyEntity.has(fieldMetadata)) continue;
            SpeedyValue speedyValue = speedyEntity.get(fieldMetadata);
            if (fieldMetadata.isAssociation()) {
                if (fieldMetadata.isCollection()) {
                    Collection<SpeedyValue> collection = speedyValue.asCollection();
                    // TODO complete collection os association
                } else {
                    EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                    SpeedyEntity valueObject = speedyValue.asObject();
                    Object pk = getPKFromSpeedyValue(valueObject, (IJpaEntityMetadata) associationMetadata);
                    Object associatedEntity = entityManager.find(associationMetadata.getEntityClass(), pk);
                    fieldMetadata.setEntityFieldWithValue(jpaEntityInstance, associatedEntity);
                }
            } else {
                if (fieldMetadata.isCollection()) {
                    Collection<SpeedyValue> collection = speedyValue.asCollection();
                    List<Object> fieldValue = collection.stream().map(item -> {
                                try {
                                    return SpeedyValueFactory.speedyValueToJavaType(speedyValue, fieldMetadata.getFieldType());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(Collectors.toList());
                    fieldMetadata.setEntityFieldWithValue(jpaEntityInstance, fieldValue);
                } else {
                    Object fieldValue = SpeedyValueFactory.speedyValueToJavaType(speedyValue, fieldMetadata.getFieldType());
                    fieldMetadata.setEntityFieldWithValue(jpaEntityInstance, fieldValue);
                }
            }
        }
    }


    public static Object getPKFromSpeedyValue(SpeedyEntity speedyEntity, IJpaEntityMetadata entityMetadata) throws Exception {
        if (entityMetadata.hasCompositeKey()) {
            Object newKeyInstance = entityMetadata.createNewKeyInstance();
            for (KeyFieldMetadata fieldMetadata : entityMetadata.getKeyFields()) {
                if (speedyEntity.has(fieldMetadata)) {
                    SpeedyValue speedyValue = speedyEntity.get(fieldMetadata);
                    Object fieldValue = SpeedyValueFactory.speedyValueToJavaType(speedyValue, fieldMetadata.getFieldType());
                    fieldMetadata.setIdFieldWithValue(newKeyInstance, fieldValue);
                }
            }
            return newKeyInstance;
        } else {
            Optional<KeyFieldMetadata> primaryKeyFieldMetadata = entityMetadata.getKeyFields().stream().findAny();
            if (primaryKeyFieldMetadata.isPresent()) {
                KeyFieldMetadata keyFieldMetadata = primaryKeyFieldMetadata.get();
                String propertyName = keyFieldMetadata.getOutputPropertyName();
                if (speedyEntity.has(keyFieldMetadata)) {
                    SpeedyValue speedyValue = speedyEntity.get(keyFieldMetadata);
                    Object fieldValue = SpeedyValueFactory.speedyValueToJavaType(speedyValue, keyFieldMetadata.getFieldType());
                    return fieldValue;
                }
            }
            throw new BadRequestException("primary key field not found" + speedyEntity);
        }
    }
}
