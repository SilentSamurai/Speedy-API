package com.github.silent.samurai.speedy.util;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CommonUtil {

    public static SpeedyEntity fromJpaEntity(Object entity, EntityMetadata entityMetadata) {
        SpeedyEntity speedyEntity = new SpeedyEntity(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            Object fieldValue = fieldMetadata.getEntityFieldValue(entity);
            if (fieldMetadata.isAssociation()) {
                if (fieldMetadata.isCollection()) {
                    Collection<?> collection = (Collection<?>) fieldValue;
                    Collection<SpeedyValue> collect = collection.stream().map(item -> {
                        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                        return fromJpaEntity(fieldValue, associationMetadata);
                    }).collect(Collectors.toList());
                    SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(collect);
                    speedyEntity.put(fieldMetadata, speedyCollection);
                } else {
                    EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                    SpeedyEntity ae = fromJpaEntity(fieldValue, associationMetadata);
                    speedyEntity.put(fieldMetadata, ae);
                }
            } else {
                if (fieldMetadata.isCollection()) {
                    Collection<?> collection = (Collection<?>) fieldValue;
                    List<SpeedyValue> collect = collection.stream().map(item -> basicToSpeedyValue(fieldMetadata.getValueType(), item))
                            .collect(Collectors.toList());
                    SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(collect);
                    speedyEntity.put(fieldMetadata, speedyCollection);
                } else {
                    SpeedyValue speedyValue = basicToSpeedyValue(fieldMetadata.getValueType(), fieldValue);
                    speedyEntity.put(fieldMetadata, speedyValue);
                }
            }
        }
        return speedyEntity;
    }

    public static SpeedyValue basicToSpeedyValue(ValueType valueType, Object instance) {
        switch (valueType) {
            case TEXT:
                return SpeedyValueFactory.fromText((String) instance);
            case INT:
                return SpeedyValueFactory.fromInt((Integer) instance);
            case FLOAT:
                return SpeedyValueFactory.fromDouble((Double) instance);
            case DATE:
                return SpeedyValueFactory.fromDate((LocalDate) instance);
            case TIME:
                return SpeedyValueFactory.fromTime((LocalTime) instance);
            case DATE_TIME:
                return SpeedyValueFactory.fromDateTime((LocalDateTime) instance);
            case NULL:
            default:
                return SpeedyValueFactory.fromNull();
        }
    }
}
