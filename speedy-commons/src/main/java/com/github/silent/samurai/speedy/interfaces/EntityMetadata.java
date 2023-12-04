package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.util.Optional;
import java.util.Set;

public interface EntityMetadata {

    String getName();

    boolean has(String fieldName);

    FieldMetadata field(String fieldName) throws NotFoundException;

    Set<FieldMetadata> getAllFields();

    Set<String> getAllFieldNames();

    boolean hasCompositeKey();

    Class<?> getEntityClass();

    Class<?> getKeyClass();

    Set<KeyFieldMetadata> getKeyFields();

    Set<String> getKeyFieldNames();

    SpeedyEntity createNewEntityInstance() throws Exception;

    SpeedyEntityKey createNewKeyInstance() throws Exception;

    Set<FieldMetadata> getAssociatedFields();

    default Optional<FieldMetadata> getAssociatedField(EntityMetadata secondaryResource) {
        return this.getAssociatedFields().stream()
                .filter(fld -> fld.getAssociationMetadata() == secondaryResource)
                .findAny();
    }
}
