package com.github.silent.samurai.speedy.util;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyValueFactory;

public class CommonUtil {

    public static SpeedyEntity fromJpaEntity(Object entity, EntityMetadata entityMetadata) {
        SpeedyEntity speedyEntity = new SpeedyEntity(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {

            if (fieldMetadata.isAssociation()) {
                if (fieldMetadata.isCollection()) {

                } else {
                    Object fieldValue = fieldMetadata.getEntityFieldValue(entity);
                    EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                    SpeedyEntity ae = fromJpaEntity(fieldValue, associationMetadata);
                    SpeedyField speedyField = SpeedyField.fromOne(fieldMetadata, fieldValue);
                }
            } else {
                if (fieldMetadata.isCollection()) {

                } else {

                }
            }

            SpeedyValue speedyValue = SpeedyValueFactory.fromOne(fieldMetadata.getValueType(), value);
            speedyEntity.put(fieldMetadata, speedyValue);
        }
        return speedyEntity;
    }
}
