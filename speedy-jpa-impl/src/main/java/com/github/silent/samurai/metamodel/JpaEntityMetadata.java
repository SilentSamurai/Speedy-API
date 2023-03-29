package com.github.silent.samurai.metamodel;

import com.github.silent.samurai.exceptions.ResourceNotFoundException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import lombok.Data;

import javax.persistence.metamodel.EntityType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class JpaEntityMetadata implements EntityMetadata {

    private String name;
    private String tableName;
    private Set<FieldMetadata> allFields = new HashSet<>();
    private Map<String, JpaFieldMetadata> fieldMap = new HashMap<>();
    boolean hasCompositeKey;
    private EntityType<?> jpaEntityType;
    private Class<?> entityClass;
    private Class<?> keyClass;
    private Set<KeyFieldMetadata> keyFields = new HashSet<>();

    @Override
    public boolean has(String fieldName) {
        return fieldMap.containsKey(fieldName);
    }

    @Override
    public FieldMetadata field(String fieldName) throws ResourceNotFoundException {
        if (has(fieldName)) {
            return fieldMap.get(fieldName);
        }
        throw new ResourceNotFoundException(name + "." + fieldName);
    }

    public Set<FieldMetadata> getAllFields() {
        return allFields;
    }

    @Override
    public Set<String> getAllFieldNames() {
        return allFields.stream()
                .map(FieldMetadata::getClassFieldName)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasCompositeKey() {
        return hasCompositeKey;
    }

    @Override
    public Set<String> getKeyFieldNames() {
        return keyFields.stream()
                .map(FieldMetadata::getClassFieldName)
                .collect(Collectors.toSet());
    }

    @Override
    public Object createNewEntityInstance() throws Exception {
        return entityClass.getConstructor().newInstance();
    }

    @Override
    public Object createNewKeyInstance() throws Exception {
        return keyClass.getConstructor().newInstance();
    }
}
