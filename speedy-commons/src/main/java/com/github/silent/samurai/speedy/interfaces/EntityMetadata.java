package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;

import java.util.Optional;
import java.util.Set;

public interface EntityMetadata {

    String getName();

    boolean has(String fieldName);

    default FieldMetadata getField(String fieldName) throws NotFoundException {
        if (has(fieldName)) {
            return field(fieldName);
        }
        throw new NotFoundException(String.format("Field '%s' not found in entity %s", fieldName, getName()));
    }

    FieldMetadata field(String fieldName) throws NotFoundException;

    Set<FieldMetadata> getAllFields();

    Set<String> getAllFieldNames();

    boolean hasCompositeKey();

    Set<KeyFieldMetadata> getKeyFields();

    Set<String> getKeyFieldNames();

    Set<FieldMetadata> getAssociatedFields();

    default Optional<FieldMetadata> getAssociatedField(EntityMetadata secondaryResource) {
        return this.getAssociatedFields().stream()
                .filter(fld -> fld.getAssociationMetadata() == secondaryResource)
                .findAny();
    }

    String getDbTableName();
}
