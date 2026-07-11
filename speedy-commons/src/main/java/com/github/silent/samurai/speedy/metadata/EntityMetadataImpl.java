package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class EntityMetadataImpl implements EntityMetadata {

    boolean hasCompositeKey = false;
    private String name;
    private String dbTableName;
    private boolean isSensitive;
    @Setter
    private TransactionMode transactionMode = TransactionMode.PER_ENTITY;
    @Setter
    private boolean bulkAllowed = false;
    @Setter
    private FieldMetadata softDeleteField = null;
    @Setter
    private boolean viewDeletedAllowed = false;
    @Setter
    private boolean hardDeleteAllowed = false;
    private Set<ActionType> actionType;
    private Map<String, FieldMetadata> fieldMap;

    private Set<FieldMetadata> allFieldsCache;
    private Set<String> allFieldNamesCache;
    private Set<KeyFieldMetadata> keyFieldsCache;
    private Set<String> keyFieldNamesCache;
    private Set<FieldMetadata> associatedFieldsCache;

    EntityMetadataImpl(String name, String dbTableName, boolean hasCompositeKey, boolean isSensitive, Set<ActionType> actionType, Map<String, FieldMetadata> fieldMap) {
        this.name = name;
        this.dbTableName = dbTableName;
        this.hasCompositeKey = hasCompositeKey;
        this.isSensitive = isSensitive;
        this.actionType = actionType == null ? null : Set.copyOf(actionType);
        this.fieldMap = fieldMap == null ? null : Map.copyOf(fieldMap);
    }

    @Override
    public boolean has(String fieldName) {
        return fieldMap.containsKey(fieldName);
    }

    @Override
    public FieldMetadata field(String fieldName) throws NotFoundException {
        if (has(fieldName)) {
            return fieldMap.get(fieldName);
        }
        throw new NotFoundException(name + "." + fieldName);
    }

    @Override
    public Set<FieldMetadata> getAllFields() {
        if (allFieldsCache == null) {
            allFieldsCache = fieldMap.values().stream()
                    .map(FieldMetadata.class::cast)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return allFieldsCache;
    }

    @Override
    public Set<String> getAllFieldNames() {
        if (allFieldNamesCache == null) {
            allFieldNamesCache = fieldMap.values().stream()
                    .map(FieldMetadata::getOutputPropertyName)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return allFieldNamesCache;
    }

    @Override
    public boolean hasCompositeKey() {
        return hasCompositeKey;
    }

    @Override
    public Set<KeyFieldMetadata> getKeyFields() {
        if (keyFieldsCache == null) {
            keyFieldsCache = fieldMap.values().stream()
                    .filter(KeyFieldMetadata.class::isInstance)
                    .map(KeyFieldMetadata.class::cast)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return keyFieldsCache;
    }

    @Override
    public Set<String> getKeyFieldNames() {
        if (keyFieldNamesCache == null) {
            keyFieldNamesCache = fieldMap.values().stream()
                    .filter(KeyFieldMetadata.class::isInstance)
                    .map(KeyFieldMetadata.class::cast)
                    .map(KeyFieldMetadata::getOutputPropertyName)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return keyFieldNamesCache;
    }

    @Override
    public Set<FieldMetadata> getAssociatedFields() {
        if (associatedFieldsCache == null) {
            associatedFieldsCache = fieldMap.values().stream()
                    .filter(FieldMetadata::isAssociation)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return associatedFieldsCache;
    }

    @Override
    public TransactionMode getTransactionMode() {
        return transactionMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityMetadataImpl that)) return false;
        return Objects.equals(dbTableName, that.dbTableName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dbTableName);
    }

}
