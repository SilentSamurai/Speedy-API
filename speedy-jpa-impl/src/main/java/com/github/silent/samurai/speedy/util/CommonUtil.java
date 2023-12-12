package com.github.silent.samurai.speedy.util;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyValueFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CommonUtil {

    public static SpeedyEntity fromJpaEntity(Object entity, EntityMetadata entityMetadata) throws Exception {
        SpeedyEntity speedyEntity = new SpeedyEntity(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            Object fieldValue = fieldMetadata.getEntityFieldValue(entity);
            if (fieldMetadata.isAssociation()) {
                if (fieldMetadata.isCollection()) {
                    Collection<?> collection = (Collection<?>) fieldValue;
                    Collection<SpeedyValue> collect = collection.stream().map(item -> {
                        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                        try {
                            return fromJpaEntity(fieldValue, associationMetadata);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
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
                    List<SpeedyValue> collect = collection.stream().map(item -> {
                                try {
                                    return SpeedyValueFactory.basicToSpeedyValue(fieldMetadata.getFieldType(), item);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(Collectors.toList());
                    SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(collect);
                    speedyEntity.put(fieldMetadata, speedyCollection);
                } else {
                    SpeedyValue speedyValue = SpeedyValueFactory.basicToSpeedyValue(fieldMetadata.getFieldType(), fieldValue);
                    speedyEntity.put(fieldMetadata, speedyValue);
                }
            }
        }
        return speedyEntity;
    }
}
