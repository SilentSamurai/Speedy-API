package com.github.silent.samurai.metamodel;

import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.metamodel.EntityType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class JpaEntityMetadata implements EntityMetadata {

    private String name;
    private String tableName;
    private Map<String, JpaFieldMetadata> fieldMap = new HashMap<>();
    boolean hasCompositeKey;
    private EntityType<?> jpaEntityType;
    private Class<?> entityClass;
    private Class<?> keyClass;

    public void addFieldMetadata(JpaFieldMetadata metadata) {
        this.fieldMap.put(metadata.getClassFieldName(), metadata);
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

    public Set<FieldMetadata> getAllFields() {
        return fieldMap.values().stream()
                .map(FieldMetadata.class::cast)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> getAllFieldNames() {
        return fieldMap.values().stream()
                .map(FieldMetadata::getClassFieldName)
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
                .map(FieldMetadata::getClassFieldName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Object createNewEntityInstance() throws Exception {
        return entityClass.getConstructor().newInstance();
    }

    @Override
    public Object createNewKeyInstance() throws Exception {
        return keyClass.getConstructor().newInstance();
    }

    @Override
    public Set<FieldMetadata> getAssociatedFields() {
        return fieldMap.values().stream()
                .filter(FieldMetadata::isAssociation)
                .collect(Collectors.toUnmodifiableSet());
    }
}
