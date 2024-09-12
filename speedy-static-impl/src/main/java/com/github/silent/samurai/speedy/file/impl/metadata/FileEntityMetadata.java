package com.github.silent.samurai.speedy.file.impl.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true, value = {
        "keyFields",
        "fieldMap"
})
public class FileEntityMetadata implements EntityMetadata {

    private String name;
    private Map<String, FileFieldMetadata> fieldMap = new HashMap<>();
    private boolean hasCompositeKey;
    private String entityType;
    private String keyType;

    public Set<FileFieldMetadata> getFields() {
        return new HashSet<>(fieldMap.values());
    }

    @Override
    public boolean has(String fieldName) {
        return fieldMap.containsKey(fieldName);
    }

    @Override
    public FieldMetadata field(String fieldName) throws NotFoundException {
        if (!fieldMap.containsKey(fieldName)) {
            throw new NotFoundException("Field " + fieldName + " not found");
        }
        return fieldMap.get(fieldName);
    }

    @Override
    public Set<FieldMetadata> getAllFields() {
        return getFields().stream().map(ffm -> (FieldMetadata) ffm).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getAllFieldNames() {
        return fieldMap.keySet();
    }

    @Override
    public boolean hasCompositeKey() {
        return hasCompositeKey;
    }

    @Override
    public Set<KeyFieldMetadata> getKeyFields() {
        return getFields().stream()
                .filter(ffm -> ffm instanceof KeyFieldMetadata)
                .map(ffm -> (KeyFieldMetadata) ffm)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> getKeyFieldNames() {
        return getFields().stream()
                .filter(ffm -> ffm instanceof KeyFieldMetadata)
                .map(FileFieldMetadata::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<FieldMetadata> getAssociatedFields() {
        return getFields().stream()
                .filter(FileFieldMetadata::isAssociation)
                .collect(Collectors.toUnmodifiableSet());
    }
}
