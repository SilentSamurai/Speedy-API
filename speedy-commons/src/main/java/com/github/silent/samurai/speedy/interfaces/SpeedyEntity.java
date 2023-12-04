package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;

import java.util.Collection;

public interface SpeedyEntity {

    EntityMetadata getMetadata();

    Collection<SpeedyEntity> getManyAssociatedValue(FieldMetadata fieldMetadata);

    SpeedyValue getBasicValue(FieldMetadata fieldMetadata);

    SpeedyValue getManyBasicValue(FieldMetadata fieldMetadata);

    SpeedyEntity getOneAssociatedValue(FieldMetadata fieldMetadata);
}
