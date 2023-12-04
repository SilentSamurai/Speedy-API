package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyEntity;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyValueImpl;

import java.util.Collection;
import java.util.stream.Collectors;

public class JpaSpeedyEntity implements SpeedyEntity {

    final Object entity;
    final EntityMetadata entityMetadata;

    public JpaSpeedyEntity(Object entity, EntityMetadata entityMetadata) {
        this.entity = entity;
        this.entityMetadata = entityMetadata;
    }

    @Override
    public EntityMetadata getMetadata() {
        return entityMetadata;
    }

    @Override
    public Collection<SpeedyEntity> getManyAssociatedValue(FieldMetadata fieldMetadata) {
        Object associatedValue = fieldMetadata.getEntityFieldValue(entity);
        if (associatedValue instanceof Collection) {
            Collection<?> values = (Collection<?>) associatedValue;
            return values.stream().map(e ->
                            new JpaSpeedyEntity(associatedValue,
                                    fieldMetadata.getAssociationMetadata()))
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public SpeedyValue getBasicValue(FieldMetadata fieldMetadata) {
        Object value = fieldMetadata.getEntityFieldValue(entity);
        return SpeedyValueImpl.fromOne(fieldMetadata.getValueType(), value);
    }

    @Override
    public SpeedyValue getManyBasicValue(FieldMetadata fieldMetadata) {
        Object entityFieldValue = fieldMetadata.getEntityFieldValue(entity);
        if (entityFieldValue instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) fieldMetadata.getEntityFieldValue(entity);
            return SpeedyValueImpl.fromMany(fieldMetadata.getValueType(), collection);
        }
        return null;
    }

    @Override
    public SpeedyEntity getOneAssociatedValue(FieldMetadata fieldMetadata) {
        Object associatedValue = fieldMetadata.getEntityFieldValue(entity);
        return new JpaSpeedyEntity(associatedValue, fieldMetadata.getAssociationMetadata());
    }
}
