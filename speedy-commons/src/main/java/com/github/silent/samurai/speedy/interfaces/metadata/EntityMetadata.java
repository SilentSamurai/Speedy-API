package com.github.silent.samurai.speedy.interfaces.metadata;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.BulkOperation;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * The Speedy-managed conditional-request token field ({@link FieldMetadata#getEtagStrategy()}),
     * if this entity declared one via {@code @SpeedyETag} (or a compatible JPA {@code @Version}).
     * Empty for every entity that did not opt in — callers on the conditional-request path must
     * short-circuit on this being empty rather than doing any extra work.
     */
    default Optional<FieldMetadata> getVersionField() {
        return getAllFields().stream().filter(f -> f.getEtagStrategy().isPresent()).findFirst();
    }

    default Set<FieldMetadata> getAllNonKeyFields() {
        return getAllFields().stream()
                .filter(fm -> !(fm instanceof KeyFieldMetadata))
                .collect(Collectors.toSet());
    }

    default Optional<FieldMetadata> getAssociatedField(EntityMetadata secondaryResource) {
        return this.getAssociatedFields().stream()
                .filter(fld -> fld.getAssociationMetadata() == secondaryResource)
                .findAny();
    }

    String getDbTableName();

    // Whether entity-level @SpeedySensitive is applied. When true, the
    // $metadata endpoint exposes this and all fields default to sensitive
    // unless individually overridden with @SpeedySensitive(false).
    default boolean isSensitive() {
        return false;
    }

    Set<ActionType> getActionType();

    default boolean isReadOnly() {
        return getActionType().contains(ActionType.READ) && getActionType().size() == 1;
    }

    default boolean isReadAllowed() {
        return getActionType().contains(ActionType.READ) || getActionType().contains(ActionType.ALL);
    }

    default boolean isCreateAllowed() {
        return getActionType().contains(ActionType.CREATE) || getActionType().contains(ActionType.ALL);
    }

    default boolean isUpdateAllowed() {
        return getActionType().contains(ActionType.UPDATE) || getActionType().contains(ActionType.ALL);
    }

    default boolean isReplaceAllowed() {
        return getActionType().contains(ActionType.REPLACE) || getActionType().contains(ActionType.ALL);
    }

    default boolean isDeleteAllowed() {
        return getActionType().contains(ActionType.DELETE) || getActionType().contains(ActionType.ALL);
    }

    default TransactionMode getTransactionMode() {
        return TransactionMode.PER_ENTITY;
    }

    // The write operations for which multi-element (bulk) request bodies are accepted,
    // as declared by @SpeedyBulk. Empty by default (bulk disabled for every operation);
    // BulkOperation.ALL enables all of them.
    default Set<BulkOperation> getBulkOperations() {
        return Collections.emptySet();
    }

    // Whether multi-element (bulk) request bodies are accepted for the given operation.
    // A body with more than one element is rejected with 400 unless this returns true.
    default boolean isBulkAllowed(BulkOperation operation) {
        Set<BulkOperation> ops = getBulkOperations();
        return ops.contains(operation) || ops.contains(BulkOperation.ALL);
    }
}
