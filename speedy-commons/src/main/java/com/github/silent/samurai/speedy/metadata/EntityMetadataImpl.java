package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class EntityMetadataImpl implements EntityMetadata {

    boolean hasCompositeKey = false;
    private String name;
    private String dbTableName;
    private Set<ActionType> actionType;
    private Map<String, FieldMetadata> fieldMap;

    EntityMetadataImpl(String name, String dbTableName, boolean hasCompositeKey, Set<ActionType> actionType, Map<String, FieldMetadata> fieldMap) {
        this.name = name;
        this.dbTableName = dbTableName;
        this.hasCompositeKey = hasCompositeKey;
        this.actionType = actionType;
        this.fieldMap = fieldMap;
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

    // TODO: improve performance
    @Override
    public Set<FieldMetadata> getAllFields() {
        return fieldMap.values().stream()
                .map(FieldMetadata.class::cast)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> getAllFieldNames() {
        return fieldMap.values().stream()
                .map(FieldMetadata::getOutputPropertyName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean hasCompositeKey() {
        return hasCompositeKey;
    }

    @Override
    public Set<KeyFieldMetadata> getKeyFields() {
        return fieldMap.values().stream()
                .filter(KeyFieldMetadata.class::isInstance)
                .map(KeyFieldMetadata.class::cast)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> getKeyFieldNames() {
        return fieldMap.values().stream()
                .filter(KeyFieldMetadata.class::isInstance)
                .map(KeyFieldMetadata.class::cast)
                .map(KeyFieldMetadata::getOutputPropertyName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<FieldMetadata> getAssociatedFields() {
        return fieldMap.values().stream()
                .filter(FieldMetadata::isAssociation)
                .collect(Collectors.toUnmodifiableSet());
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
