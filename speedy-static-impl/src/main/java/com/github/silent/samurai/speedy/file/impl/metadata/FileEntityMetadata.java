package com.github.silent.samurai.speedy.file.impl.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true, value = {
        "keyFields",
        "fieldMap"
})
public class FileEntityMetadata implements EntityMetadata {

    private String name;
    private Set<FileFieldMetadata> fields;
    private Map<String, FileFieldMetadata> fieldMap = new HashMap<>();
    private boolean hasCompositeKey;
    private String entityType;
    private String keyType;

    public void setFields(Set<FileFieldMetadata> fields) {
        this.fields = fields;
        fields.forEach(field -> fieldMap.put(field.getName(), field));
    }

    @Override
    public boolean has(String fieldName) {
        return fieldMap.containsKey(fieldName);
    }

    @Override
    public FieldMetadata field(String fieldName) throws NotFoundException {
        return fieldMap.get(fieldName);
    }

    @Override
    public Set<FieldMetadata> getAllFields() {
        return Set.of();
    }

    @Override
    public Set<String> getAllFieldNames() {
        return Set.of();
    }

    @Override
    public boolean hasCompositeKey() {
        return false;
    }

    @Override
    public Set<KeyFieldMetadata> getKeyFields() {
        return Set.of();
    }

    @Override
    public Set<String> getKeyFieldNames() {
        return Set.of();
    }

    @Override
    public Set<FieldMetadata> getAssociatedFields() {
        return Set.of();
    }
}
