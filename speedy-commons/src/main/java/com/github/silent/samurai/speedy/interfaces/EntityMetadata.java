package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.annotations.SpeedyAction;
import com.github.silent.samurai.speedy.enums.ActionType;
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

    ActionType getActionType();

    default boolean isReadOnly() {
        return getActionType() == ActionType.READ;
    }

    default boolean isReadAllowed() {
        return getActionType() == ActionType.READ || getActionType() == ActionType.ALL;
    }

    default boolean isCreateAllowed() {
        return getActionType() == ActionType.CREATE || getActionType() == ActionType.ALL;
    }

    default boolean isUpdateAllowed() {
        return getActionType() == ActionType.UPDATE || getActionType() == ActionType.ALL;
    }

    default boolean isDeleteAllowed() {
        return getActionType() == ActionType.DELETE || getActionType() == ActionType.ALL;
    }
}
