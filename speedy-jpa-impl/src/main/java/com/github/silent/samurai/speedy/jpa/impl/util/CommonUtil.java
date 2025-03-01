package com.github.silent.samurai.speedy.jpa.impl.util;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaEntityMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaFieldMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaKeyFieldMetadata;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;

import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

public class CommonUtil {


    public static SpeedyEntity fromJpaEntity(Object entity, EntityMetadata entityMetadata, Set<String> expands) throws Exception {
        return fromJpaEntityInner(entity, entityMetadata, expands);
    }

    private static SpeedyEntity fromJpaEntityInner(Object entity, EntityMetadata entityMetadata, Set<String> expands) throws Exception {
        SpeedyEntity speedyEntity = SpeedyValueFactory.fromEntityMetadata(entityMetadata);
        for (FieldMetadata simpleMetadata : entityMetadata.getAllFields()) {
            IJpaFieldMetadata fieldMetadata = (IJpaFieldMetadata) simpleMetadata;
            Object fieldValue = fieldMetadata.getEntityFieldValue(entity);
            if (fieldValue == null) {
                speedyEntity.put(fieldMetadata, SpeedyValueFactory.fromNull());
                continue;
            }

            if (fieldMetadata.isAssociation()) {
                if (expands.contains(fieldMetadata.getAssociationMetadata().getName())) {
                    if (fieldMetadata.isCollection()) {
                        Collection<?> collection = (Collection<?>) fieldValue;
                        Collection<SpeedyValue> collect = new LinkedList<>();
                        for (Object item : collection) {
                            EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                            SpeedyEntity associatedEntity = fromJpaEntityInner(item, associationMetadata, expands);
                            collect.add(associatedEntity);
                        }
                        SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(collect);
                        speedyEntity.put(fieldMetadata, speedyCollection);
                    } else {
                        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                        SpeedyEntity associatedEntity = fromJpaEntityInner(fieldValue, associationMetadata, expands);
                        speedyEntity.put(fieldMetadata, associatedEntity);
                    }
                } else {
                    if (fieldMetadata.isCollection()) {
                        Collection<?> collection = (Collection<?>) fieldValue;
                        Collection<SpeedyValue> collect = new LinkedList<>();
                        for (Object item : collection) {
                            EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                            SpeedyEntity associatedEntity = fromKey(item, associationMetadata);
                            collect.add(associatedEntity);
                        }
                        SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(collect);
                        speedyEntity.put(fieldMetadata, speedyCollection);
                    } else {
                        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                        SpeedyEntityKey associatedEntity = fromKey(fieldValue, associationMetadata);
                        speedyEntity.put(fieldMetadata, associatedEntity);
                    }
                }
            } else {
                if (fieldMetadata.isCollection()) {
                    Collection<?> listOfInstances = (Collection<?>) fieldValue;
                    List<SpeedyValue> listOfSpeedyValue = new LinkedList<>();
                    for (Object item : listOfInstances) {
                        SpeedyValue speedyValue = SpeedyValueFactory.toSpeedyValue(fieldMetadata, item);
                        listOfSpeedyValue.add(speedyValue);
                    }
                    SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(listOfSpeedyValue);
                    speedyEntity.put(fieldMetadata, speedyCollection);
                } else {
                    SpeedyValue speedyValue = SpeedyValueFactory.toSpeedyValue(fieldMetadata, fieldValue);
                    speedyEntity.put(fieldMetadata, speedyValue);
                }
            }
        }
        return speedyEntity;
    }


    public static SpeedyEntityKey fromKey(Object entity, EntityMetadata entityMetadata) throws SpeedyHttpException {
        SpeedyEntityKey speedyEntityKey = new SpeedyEntityKey(entityMetadata);
        for (KeyFieldMetadata simpleMetadata : entityMetadata.getKeyFields()) {
            IJpaKeyFieldMetadata keyFieldMetadata = (IJpaKeyFieldMetadata) simpleMetadata;
            Object fieldValue;
            if (entityMetadata.hasCompositeKey()) {
                fieldValue = keyFieldMetadata.getEntityFieldValue(entity);
            } else {
                fieldValue = keyFieldMetadata.getEntityFieldValue(entity);
            }

            if (fieldValue == null) {
                speedyEntityKey.put(keyFieldMetadata, SpeedyValueFactory.fromNull());
                continue;
            }
            SpeedyValue speedyValue = SpeedyValueFactory.toSpeedyValue(keyFieldMetadata, fieldValue);
            speedyEntityKey.put(keyFieldMetadata, speedyValue);
        }
        return speedyEntityKey;
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
        for (FieldMetadata simpleMetadata : entityMetadata.getAllFields()) {
            IJpaFieldMetadata fieldMetadata = (IJpaFieldMetadata) simpleMetadata;
            if (!speedyEntity.has(fieldMetadata)) continue;
            SpeedyValue speedyValue = speedyEntity.get(fieldMetadata);
            if (fieldMetadata.isAssociation()) {
                if (fieldMetadata.isCollection()) {
                    Collection<SpeedyValue> collection = speedyValue.asCollection();
                    // TODO complete collection os association
                } else {
                    IJpaEntityMetadata associationMetadata = (IJpaEntityMetadata) fieldMetadata.getAssociationMetadata();
                    SpeedyEntity valueObject = speedyValue.asObject();
                    Object pk = getPKFromSpeedyValue(valueObject, associationMetadata);
                    Object associatedEntity = entityManager.find(associationMetadata.getEntityClass(), pk);
                    fieldMetadata.setEntityFieldWithValue(jpaEntityInstance, associatedEntity);
                }
            } else {
                if (fieldMetadata.isCollection()) {
                    Collection<SpeedyValue> collection = speedyValue.asCollection();
                    List<Object> fieldValue = collection.stream().map(item -> {
                                try {
                                    return SpeedyValueFactory.toJavaType(fieldMetadata, speedyValue);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(Collectors.toList());
                    fieldMetadata.setEntityFieldWithValue(jpaEntityInstance, fieldValue);
                } else {
                    Object fieldValue = SpeedyValueFactory.toJavaType(fieldMetadata, speedyValue);
                    fieldMetadata.setEntityFieldWithValue(jpaEntityInstance, fieldValue);
                }
            }
        }
    }


    public static Object getPKFromSpeedyValue(SpeedyEntity speedyEntity, IJpaEntityMetadata entityMetadata) throws Exception {
        if (entityMetadata.hasCompositeKey()) {
            Object newKeyInstance = entityMetadata.createNewKeyInstance();
            for (KeyFieldMetadata simpleMetadata : entityMetadata.getKeyFields()) {
                IJpaKeyFieldMetadata fieldMetadata = (IJpaKeyFieldMetadata) simpleMetadata;
                if (speedyEntity.has(fieldMetadata)) {
                    SpeedyValue speedyValue = speedyEntity.get(fieldMetadata);
                    Object fieldValue = SpeedyValueFactory.toJavaType(fieldMetadata, speedyValue);
                    fieldMetadata.setIdFieldWithValue(newKeyInstance, fieldValue);
                }
            }
            return newKeyInstance;
        } else {
            Optional<KeyFieldMetadata> primaryKeyFieldMetadata = entityMetadata.getKeyFields().stream().findAny();
            if (primaryKeyFieldMetadata.isPresent()) {
                KeyFieldMetadata keyFieldMetadata = primaryKeyFieldMetadata.get();
                if (speedyEntity.has(keyFieldMetadata)) {
                    SpeedyValue speedyValue = speedyEntity.get(keyFieldMetadata);
                    Object fieldValue = SpeedyValueFactory.toJavaType(keyFieldMetadata, speedyValue);
                    return fieldValue;
                }
            }
            throw new BadRequestException("primary key field not found" + speedyEntity);
        }
    }
}
