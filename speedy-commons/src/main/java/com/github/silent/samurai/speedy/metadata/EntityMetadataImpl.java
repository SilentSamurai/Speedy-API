package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.BulkOperation;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class EntityMetadataImpl implements EntityMetadata {

    boolean hasCompositeKey = false;
    private final String name;
    private final String dbTableName;
    private final boolean isSensitive;
    private Set<BulkOperation> bulkOperations = Collections.emptySet();
    private final Set<ActionType> actionType;
    private final Map<String, FieldMetadata> fieldMap;

    private Set<FieldMetadata> allFieldsCache;
    private Set<String> allFieldNamesCache;
    private Set<KeyFieldMetadata> keyFieldsCache;
    private Set<String> keyFieldNamesCache;
    private Set<FieldMetadata> associatedFieldsCache;
    private Optional<FieldMetadata> versionFieldCache;

    EntityMetadataImpl(String name, String dbTableName, boolean hasCompositeKey, boolean isSensitive, Set<ActionType> actionType, Map<String, FieldMetadata> fieldMap) {
        this.name = name;
        this.dbTableName = dbTableName;
        this.hasCompositeKey = hasCompositeKey;
        this.isSensitive = isSensitive;
        this.actionType = actionType == null ? null : Set.copyOf(actionType);
        // Map.copyOf/Set.copyOf make no iteration-order guarantee; a LinkedHashMap/LinkedHashSet
        // copy preserves the caller's (declaration) order, which OASGenerator relies on when
        // emitting composite-key parameters — that order becomes the generated Java client's
        // positional method arguments.
        this.fieldMap = fieldMap == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(fieldMap));
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
            LinkedHashSet<FieldMetadata> ordered = fieldMap.values().stream()
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            allFieldsCache = Collections.unmodifiableSet(ordered);
        }
        return allFieldsCache;
    }

    @Override
    public Set<String> getAllFieldNames() {
        if (allFieldNamesCache == null) {
            LinkedHashSet<String> ordered = fieldMap.values().stream()
                    .map(FieldMetadata::getOutputPropertyName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            allFieldNamesCache = Collections.unmodifiableSet(ordered);
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
            LinkedHashSet<KeyFieldMetadata> ordered = fieldMap.values().stream()
                    .filter(KeyFieldMetadata.class::isInstance)
                    .map(KeyFieldMetadata.class::cast)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            keyFieldsCache = Collections.unmodifiableSet(ordered);
        }
        return keyFieldsCache;
    }

    @Override
    public Set<String> getKeyFieldNames() {
        if (keyFieldNamesCache == null) {
            LinkedHashSet<String> ordered = fieldMap.values().stream()
                    .filter(KeyFieldMetadata.class::isInstance)
                    .map(KeyFieldMetadata.class::cast)
                    .map(KeyFieldMetadata::getOutputPropertyName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            keyFieldNamesCache = Collections.unmodifiableSet(ordered);
        }
        return keyFieldNamesCache;
    }

    @Override
    public Set<FieldMetadata> getAssociatedFields() {
        if (associatedFieldsCache == null) {
            LinkedHashSet<FieldMetadata> ordered = fieldMap.values().stream()
                    .filter(FieldMetadata::isAssociation)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            associatedFieldsCache = Collections.unmodifiableSet(ordered);
        }
        return associatedFieldsCache;
    }

    public void setBulkOperations(Set<BulkOperation> bulkOperations) {
        this.bulkOperations = bulkOperations == null ? Collections.emptySet() : Set.copyOf(bulkOperations);
    }

    @Override
    public Optional<FieldMetadata> getVersionField() {
        if (versionFieldCache == null || versionFieldCache.isEmpty()) {
            versionFieldCache = fieldMap.values().stream().filter(f -> f.getEtagStrategy().isPresent()).findFirst();
        }
        return versionFieldCache;
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
